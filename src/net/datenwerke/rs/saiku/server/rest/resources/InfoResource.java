/*
 *   Copyright 2012 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package net.datenwerke.rs.saiku.server.rest.resources;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.saiku.service.PlatformUtilsService;
import org.saiku.service.util.dto.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Info Resource to get platform information.
 */
@Path("/saiku/info")
@XmlAccessorType(XmlAccessType.NONE)
public class InfoResource {

   private static final Logger log = LoggerFactory.getLogger(InfoResource.class);

   private PlatformUtilsService platformService;

   // @Autowired
//  public void setPlatformUtilsService(Object ps) {
////    this.platformService = ps;
//  }

   @Inject
   public InfoResource(PlatformUtilsService platformService) {
      this.platformService = platformService;
   }

   /**
    * Get a list of available plugins.
    * 
    * @return A response containing a list of plugins.
    */
   @GET
   @Produces({ "application/json" })
//  @ReturnType("java.util.List<Plugin>")
   public Response getAvailablePlugins() {

      GenericEntity<List<Plugin>> entity = new GenericEntity<List<Plugin>>(platformService.getAvailablePlugins()) {
      };
      return Response.ok(entity).build();
   }

}
