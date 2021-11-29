package net.datenwerke.rs.core.service.mail;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static net.datenwerke.rs.utils.exception.shared.LambdaExceptionUtil.rethrowFunction;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.datenwerke.rs.core.client.RsCoreUiModule;
import net.datenwerke.rs.core.service.datasinkmanager.DatasinkService;
import net.datenwerke.rs.core.service.mail.annotations.MailModuleProperties;
import net.datenwerke.rs.core.service.mail.events.SendMailEvent;
import net.datenwerke.rs.core.service.mail.exceptions.MailerRuntimeException;
import net.datenwerke.rs.core.service.mail.interfaces.NeedsPostprocessing;
import net.datenwerke.rs.core.service.mail.interfaces.SessionProvider;
import net.datenwerke.rs.emaildatasink.service.emaildatasink.EmailDatasinkService;
import net.datenwerke.rs.emaildatasink.service.emaildatasink.EmailDatasinkSessionFactory;
import net.datenwerke.rs.emaildatasink.service.emaildatasink.definitions.EmailDatasink;
import net.datenwerke.rs.utils.eventbus.EventBus;
import net.datenwerke.rs.utils.juel.SimpleJuel;
import net.datenwerke.security.service.crypto.CryptoService;
import net.datenwerke.security.service.usermanager.entities.User;

/**
 * 
 *
 */
public class MailServiceImpl implements MailService {

   private final Logger logger = LoggerFactory.getLogger(getClass().getName());

   private final static String CFG_ENCRYPTION_POLICY = "mail.encryptionPolicy";
   private final static String CFG_FORCE_SENDER = "mail.forceSender";

   public interface MailSupervisor {
      void handleException(Exception e);

      void handleTrace(String trace);
   }

   public class MailSupervisorImpl implements MailSupervisor {
      @Override
      public void handleTrace(String trace) {
      }

      @Override
      public void handleException(Exception e) {
         // TODO: throw exception
         throw new MailerRuntimeException("Mail could not be send", e);
      }
   }

   private final Provider<SimpleMailFactory> simpleMailFactoryProvider;
   private final Provider<SimpleCryptoMailFactory> simpleCryptoMailFactoryProvider;
   private final CryptoService cryptoService;
   private final Provider<SimpleJuel> simpleJuelProvider;
   private final Provider<EventBus> eventBus;
   private final Provider<Configuration> config;

   // used when sending emails without email datasinks
   private final Provider<Session> defaultSessionProvider;
   // used when sending emails based on email datasinks
   private final Provider<EmailDatasinkSessionFactory> emailDatasinkSessionFactory;
   
   private final Provider<EmailDatasinkService> emailDatasinkServiceProvider;
   private final Provider<DatasinkService> datasinkServiceProvider;

   /* thread pool used to send mails asynchronously */
   private final ExecutorService sendMailPool = Executors.newFixedThreadPool(1);

   @Inject
   public MailServiceImpl(
         Provider<Session> defaultSessionProvider,
         Provider<EmailDatasinkSessionFactory> emailDatasinkSessionFactory,
         Provider<SimpleMailFactory> simpleMailFactoryProvider,
         Provider<SimpleCryptoMailFactory> simpleCryptoMailFactoryProvider, 
         CryptoService cryptoService,
         Provider<SimpleJuel> simpleJuelProvider, 
         Provider<EventBus> eventBus,
         Provider<EmailDatasinkService> emailDatasinkServiceProvider,
         @MailModuleProperties Provider<Configuration> config,
         Provider<DatasinkService> datasinkServiceProvider
         ) {

      /* store objects */
      this.defaultSessionProvider = defaultSessionProvider;
      this.emailDatasinkSessionFactory = emailDatasinkSessionFactory;
      this.simpleMailFactoryProvider = simpleMailFactoryProvider;
      this.simpleCryptoMailFactoryProvider = simpleCryptoMailFactoryProvider;
      this.cryptoService = cryptoService;
      this.simpleJuelProvider = simpleJuelProvider;
      this.eventBus = eventBus;
      this.emailDatasinkServiceProvider = emailDatasinkServiceProvider;
      this.config = config;
      this.datasinkServiceProvider = datasinkServiceProvider;
   }

   @Override
   public SimpleMail newSimpleMail() {
      return newSimpleMail(Optional.empty());
   }

   @Override
   public SimpleMail newSimpleMail(Optional<EmailDatasink> emailDatasink) {
      if (!emailDatasink.isPresent()) {
         //try to load default
         Optional<EmailDatasink> defaultEmailDatasink = loadDefaultEmailDatasink();
         if (defaultEmailDatasink.isPresent())
            return simpleCryptoMailFactoryProvider.get()
                  .create(emailDatasinkSessionFactory.get().create(defaultEmailDatasink.get()).get());
         else
            return simpleCryptoMailFactoryProvider.get().create(defaultSessionProvider.get());
      } else
         return simpleCryptoMailFactoryProvider.get()
               .create(emailDatasinkSessionFactory.get().create(emailDatasink.get()).get());
   }

   @Override
   public SimpleMail newTemplateMail(MailTemplate template, SimpleAttachment... attachments) {
      return newTemplateMail(Optional.empty(), template, attachments);
   }

   @Override
   public SimpleMail newTemplateMail(Optional<EmailDatasink> emailDatasink, MailTemplate template,
         SimpleAttachment... attachments) {
      SimpleMail mail = newSimpleMail(emailDatasink);
      SimpleJuel juel = simpleJuelProvider.get();

      template.configureMail(mail, juel, attachments);

      return mail;
   }

   @Override
   public synchronized void sendMailSync(MimeMessage message) {
      sendMailSync(message, new MailSupervisorImpl());
   }

   @Override
   public synchronized void sendMailSync(MimeMessage message, MailSupervisor supervisor) {
      sendMailSync(Optional.empty(), message, supervisor);
   }

   @Override
   public void sendMailSync(Optional<EmailDatasink> emailDatasink, MimeMessage message) {
      sendMailSync(emailDatasink, message, new MailSupervisorImpl());
   }
   
   /**
    * Partitions the given addresses in addresses supporting encryption and addresses not
    * supporting encryption. Adds the partitions to the given collections.
    * 
    * @param allAddresses all addresses
    * @param supported container for adding all addresses supporting encryption
    * @param unsupported container for adding all addresses not supporting encryption
    */
   private void partitionEncryptionSupportedAddresses(Collection<Address> allAddresses, 
         Collection<Address> supported, Collection<Address> unsupported) {
      Objects.requireNonNull(allAddresses);
      Objects.requireNonNull(supported);
      Objects.requireNonNull(unsupported);
      
      Map<Boolean, List<Address>> partitioned = allAddresses
            .stream()
            .collect(partitioningBy(a -> null != cryptoService.getUserCryptoCredentials(a.toString())));
      
      supported.addAll(partitioned.get(true));
      unsupported.addAll(partitioned.get(false));
   }
   
   private Optional<EmailDatasink> loadDefaultEmailDatasink() {
      // try to load default email datasink
      Optional<EmailDatasink> defaultEmailDatasink = (Optional<EmailDatasink>) datasinkServiceProvider.get()
            .getDefaultDatasink(emailDatasinkServiceProvider.get());
      if (defaultEmailDatasink.isPresent())
         return defaultEmailDatasink;

      return Optional.empty();
   }
   
   @Override
   public synchronized void sendMailSync(Optional<EmailDatasink> emailDatasink, MimeMessage message,
         MailSupervisor supervisor) {
      eventBus.get().fireEvent(new SendMailEvent(message));
      
      Optional<EmailDatasink> toUse = emailDatasink;
      if (!toUse.isPresent()) 
         toUse = loadDefaultEmailDatasink();

      try {
         /* check crypto policy */
         final List<Address> rcptTOencSupported = new ArrayList<>();
         final List<Address> rcptTOencNotSupported = new ArrayList<>();
         final List<Address> rcptCCencSupported = new ArrayList<>();
         final List<Address> rcptCCencNotSupported = new ArrayList<>();
         final List<Address> rcptBCCencSupported = new ArrayList<>();
         final List<Address> rcptBCCencNotSupported = new ArrayList<>();

         if (null != message.getRecipients(RecipientType.TO)) {
            partitionEncryptionSupportedAddresses(Arrays.asList(message.getRecipients(RecipientType.TO)),
                  rcptTOencSupported, rcptTOencNotSupported);
         }
         if (null != message.getRecipients(RecipientType.CC)) {
            partitionEncryptionSupportedAddresses(Arrays.asList(message.getRecipients(RecipientType.CC)),
                  rcptCCencSupported, rcptCCencNotSupported);
         }
         if (null != message.getRecipients(RecipientType.BCC)) {
            partitionEncryptionSupportedAddresses(Arrays.asList(message.getRecipients(RecipientType.BCC)),
                  rcptBCCencSupported, rcptBCCencNotSupported);
         }

         /* if there are recipients that do not support encrypted communication */
         if (!(rcptTOencNotSupported.isEmpty() && rcptCCencNotSupported.isEmpty()
               && rcptBCCencNotSupported.isEmpty())) {
            if (forceEncryption(toUse)) {
               /* remove and warn */
               logger.warn("Some recipients were removed from a message, because no public key was found. "
                     + rcptTOencNotSupported + ", " + rcptCCencNotSupported + ", " + rcptBCCencNotSupported);
               if (rcptTOencSupported.isEmpty()) {
                  throw new RuntimeException(
                        "no recipients found that support encryption, but strict encryption policy was configured");
               }
               message.setRecipients(RecipientType.TO, rcptTOencSupported.toArray(new Address[0]));
               message.setRecipients(RecipientType.CC, rcptCCencSupported.toArray(new Address[0]));
               message.setRecipients(RecipientType.BCC, rcptBCCencSupported.toArray(new Address[0]));
            } else {
               SimpleMail plaintextMail = null;

               if (!toUse.isPresent())
                  plaintextMail = simpleMailFactoryProvider.get().create(defaultSessionProvider.get());
               else
                  plaintextMail = simpleMailFactoryProvider.get()
                        .create(emailDatasinkSessionFactory.get().create(toUse.get()).get());

               if (rcptTOencSupported.isEmpty()) {
                  /* only send plaintext */
                  plaintextMail.addRecipients(RecipientType.TO, rcptTOencSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.TO, rcptTOencNotSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.CC, rcptCCencSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.CC, rcptCCencNotSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.BCC, rcptBCCencSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.BCC, rcptBCCencNotSupported.toArray(new Address[0]));

                  plaintextMail.setFrom(message.getFrom()[0]);
                  plaintextMail.setSubject(message.getSubject());

                  if (message instanceof SimpleCryptoMail) {
                     MimeMultipart mmp = new MimeMultipart();
                     mmp.addBodyPart(((SimpleCryptoMail) message).rootBodyPart);
                     plaintextMail.setContent(mmp);
                  } else {
                     plaintextMail.setContent((Multipart) message.getContent());
                  }

                  message = plaintextMail;
               } else {
                  /* split message */
                  plaintextMail.addRecipients(RecipientType.TO, rcptTOencNotSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.CC, rcptCCencNotSupported.toArray(new Address[0]));
                  plaintextMail.addRecipients(RecipientType.BCC, rcptBCCencNotSupported.toArray(new Address[0]));

                  plaintextMail.setFrom(message.getFrom()[0]);
                  plaintextMail.setSubject(message.getSubject());

                  if (message instanceof SimpleCryptoMail) {
                     MimeMultipart mmp = new MimeMultipart();
                     mmp.addBodyPart(((SimpleCryptoMail) message).rootBodyPart);
                     plaintextMail.setContent(mmp);
                  } else {
                     plaintextMail.setContent((Multipart) message.getContent());
                  }

                  sendMailSync(plaintextMail);

                  message.setRecipients(RecipientType.TO, rcptTOencSupported.toArray(new Address[0]));
                  message.setRecipients(RecipientType.CC, rcptCCencSupported.toArray(new Address[0]));
                  message.setRecipients(RecipientType.BCC, rcptBCCencSupported.toArray(new Address[0]));
               }
            }
         }

         if (message instanceof NeedsPostprocessing) {
            ((NeedsPostprocessing) message).postprocess();
         }

         boolean oldDebug = false;
         PrintStream oldOut = null;
         ByteArrayOutputStream newOut = new ByteArrayOutputStream();

         if (message instanceof SessionProvider) {
            oldDebug = ((SessionProvider) message).getSession().getDebug();
            oldOut = ((SessionProvider) message).getSession().getDebugOut();
            ((SessionProvider) message).getSession().setDebugOut(new PrintStream(newOut));
            ((SessionProvider) message).getSession().setDebug(true);
         }

         if (forceDefaultSender(toUse)) {
            String senderName = getSenderName(toUse);
            if (null == senderName)
               message.setFrom(new InternetAddress(getSender(toUse)));
            else
               message.setFrom(new InternetAddress(getSender(toUse), senderName));
         }

         message.setSentDate(new Date());

         Transport.send(message);

         if (message instanceof SessionProvider) {
            ((SessionProvider) message).getSession().setDebug(oldDebug);
            ((SessionProvider) message).getSession().setDebugOut(oldOut);

            supervisor.handleTrace(newOut.toString());
         } else
            supervisor.handleTrace("successs");
      } catch (Exception e) {
         supervisor.handleException(e);
      }
   }

   @Override
   public void sendMail(final MimeMessage message) {
      sendMail(message, new MailSupervisorImpl());
   }

   @Override
   public void sendMail(Optional<EmailDatasink> emailDatasink, MimeMessage message) {
      sendMail(emailDatasink, message, new MailSupervisorImpl());
   }

   private String getSender(Optional<EmailDatasink> emailDatasink) {
      if (!emailDatasink.isPresent())
         return config.get().getString(MailModule.PROPERTY_MAIL_SENDER, null);
      else
         return emailDatasink.get().getSender();
   }

   private String getSenderName(Optional<EmailDatasink> emailDatasink) {
      if (!emailDatasink.isPresent())
         return config.get().getString(MailModule.PROPERTY_MAIL_SENDER_NAME, null);
      else
         return emailDatasink.get().getSenderName();
   }

   private boolean forceDefaultSender(Optional<EmailDatasink> emailDatasink) {
      if (!emailDatasink.isPresent())
         return !config.get().getString(CFG_FORCE_SENDER, "false").equals("false");
      else
         return emailDatasink.get().isForceSender();
   }

   private boolean forceEncryption(Optional<EmailDatasink> emailDatasink) {
      if (!emailDatasink.isPresent()) {
         return !config.get().getString(CFG_ENCRYPTION_POLICY, 
               RsCoreUiModule.EMAIL_ENCRYPTION_MIXED).equals(RsCoreUiModule.EMAIL_ENCRYPTION_MIXED);
      } else
         return !emailDatasink.get().getEncryptionPolicy().equals(RsCoreUiModule.EMAIL_ENCRYPTION_MIXED);
   }

   @Override
   public void sendMail(final MimeMessage message, final MailSupervisor supervisor) {
      sendMail(Optional.empty(), message, supervisor);
   }

   @Override
   public void sendMail(final Optional<EmailDatasink> emailDatasink, final MimeMessage message,
         final MailSupervisor supervisor) {
      sendMailPool.execute(() -> {
         try {
            sendMailSync(emailDatasink, message, supervisor);
         } catch (Exception e) {
            /* print stack trace to server log */
            logger.warn(e.getMessage(), e);
         }
      });
   }

   @Override
   public List<Address> getEmailList(List<User> users) throws AddressException {
      return users
            .stream()
            .filter(user -> null != user.getEmail() && !"".equals(user.getEmail()))
            .map(rethrowFunction(user -> new InternetAddress(user.getEmail()))).distinct()
            .collect(toList());
   }

}
