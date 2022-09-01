package net.datenwerke.rs.rest.resources;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.datenwerke.rs.adminutils.service.systemconsole.generalinfo.GeneralInfoService;
import net.datenwerke.rs.adminutils.service.systemconsole.genrights.SystemConsoleSecurityTarget;
import net.datenwerke.rs.core.server.reportexport.helper.ApiKeyHelper;
import net.datenwerke.security.service.authenticator.AuthenticatorService;
import net.datenwerke.security.service.security.SecurityService;
import net.datenwerke.security.service.security.rights.Read;
import net.datenwerke.security.service.usermanager.entities.User;

@Path("/general-info")
public class GeneralInfoResource {

   private final Provider<GeneralInfoService> generalInfoServiceProvider;
   private final Provider<AuthenticatorService> authenticatorServiceProvider;
   private final Provider<ApiKeyHelper> apiKeyHelperProvider;
   private final Provider<SecurityService> securityServiceProvider;
   
   @Context
   private HttpServletRequest request;
   
   @Inject
   public GeneralInfoResource(
         Provider<GeneralInfoService> generalInfoServiceProvider,
         Provider<AuthenticatorService> authenticatorServiceProvider,
         Provider<ApiKeyHelper> apiKeyHelperProvider,
         Provider<SecurityService> securityServiceProvider
         ) {
      this.generalInfoServiceProvider = generalInfoServiceProvider;
      this.authenticatorServiceProvider = authenticatorServiceProvider;
      this.apiKeyHelperProvider = apiKeyHelperProvider;
      this.securityServiceProvider = securityServiceProvider;
   }
   
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response getGeneralInfo() {
      try {
         final ApiKeyHelper apiKeyHelper = apiKeyHelperProvider.get();
         String apikey = apiKeyHelper.readApiKeyFromBearer(request);
         if (null == apikey)
            apikey = request.getParameter("apikey");
         final User user = apiKeyHelper.getUser(request, apikey);
         if (null != user) {
            try {
               authenticatorServiceProvider.get().setAuthenticatedInThread(user.getId());
               securityServiceProvider.get().assertRights(SystemConsoleSecurityTarget.class, Read.class);
   
               return Response.ok().entity(generalInfoServiceProvider.get().getGeneralInfo()).build();
            } finally {
               authenticatorServiceProvider.get().logoffUserInThread();
            }
         } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
         }
         
      } catch (Exception e) {
         return Response.status(Response.Status.UNAUTHORIZED).build();
      }
   }
   
}
