/***********************************************************************
 * Copyright (c) 2015:
 * Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package it.infn.ct.futuregateway.apiserver.inframanager;

import it.infn.ct.futuregateway.apiserver.inframanager.gLite.Defaults;
import it.infn.ct.futuregateway.apiserver.resources.Infrastructure;
import it.infn.ct.futuregateway.apiserver.resources.Params;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;

/**
 * Build a valid session for the requested infrastructure.
 *
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public final class SessionBuilder {
    /**
     * Logger object.
     * Based on apache commons logging.
     */
    private final Log log = LogFactory.getLog(SessionBuilder.class);

    /**
     * Parameters to use for the session.
     */
    private Map<String, Params> params = new HashMap<>();

    /**
     * Infrastructure requiring the session.
     */
    private Infrastructure infrastructure;

    /**
     * User requiring the session.
     */
    private String user;

    /**
     * This class cannot be instantiated.
     */
    public SessionBuilder() {
    }


    /**
     * Build a new session.
     * The session is specifically created for the infrastructure provided.
     * <p>
     * This is aquivalent to {@code SessionBuilder(anInfra, null)}
     *
     * @param anInfra The infrastructure associated with the configured session
     */
    public SessionBuilder(final Infrastructure anInfra) {
        this(anInfra, null);
    }

    /**
     * Build a new session.
     * The session is specifically created for the infrastructure provided.
     *
     * @param anInfra The infrastructure associated with the configured session
     * @param aUser The user requesting the session
     */
    public SessionBuilder(final Infrastructure anInfra, final String aUser) {
        this.infrastructure = anInfra;
        for (Params par: anInfra.getParameters()) {
            params.put(par.getName(), par);
        }
        this.user = aUser;
    }


    /**
     * Build a new session.
     *
     * @param someParams Parameters to use for the new session
     */
    public SessionBuilder(final Map<String, Params> someParams) {
        this(someParams, null);
    }


    /**
     * Build a new session.
     *
     * @param someParams Parameters to use for the new session
     * @param aUser The user requesting the session
     */
    public SessionBuilder(final Map<String, Params> someParams,
            final String aUser) {
        this.params = someParams;
        this.user = aUser;
    }


    /**
     * Add additional parameters.
     *
     * @param aParam The new parameter
     */
    public void addParameter(final Params aParam) {
        this.params.put(aParam.getName(), aParam);
    }


    /**
     * Create a new session.
     * The session is create with the context derived from the infrastructure
     * parameters.
     *
     * @return The new session
     * @throws InfrastructureException In case there is a problem with the
     * infrastructure or the parameter are not correct for the context (bad or
     * missed values)
     */
    public Session createSession()
            throws InfrastructureException {
        Session newSession;
        try {
            newSession = SessionFactory.createSession(false);
        } catch (NoSuccessException nse) {
            log.error("Impossible to generate a new session.");
            log.error(nse);
            throw new InfrastructureException("New sessions cannot be created");
        }
        String  infratype;
        if (params.containsKey("type")) {
            infratype = params.get("type").getValue();
        } else {
            infratype = params.get("jobservice").getValue();
            infratype = infratype.substring(0, infratype.indexOf(":"));
        }
        log.debug("Create a new session for the type: " + infratype);
        Context context = null;
        try {
            switch (infratype) {
                case "wsgram":
                case "gatekeeper":
                    context = ContextFactory.createContext("VOMS");
                    context.setAttribute(Context.USERPROXY,
                            readRemoteProxy());
                    break;
                case "wms":
                    context = ContextFactory.createContext("VOMS");
                    context.setAttribute(Context.USERPROXY,
                            readRemoteProxy());
                    context.setVectorAttribute("JobServiceAttributes",
                    new String[] {
                        "wms.RetryCount=" + getParamterValue("retrycount",
                                Integer.toString(Defaults.RETRYCOUNT)),
                        "wms.rank=other.GlueCEStateFreeCPUs",
                        "wms.MyProxyServer=" + getParamterValue("myproxyserver",
                                "") });
                    break;
                case "occi":
                case "rocci":
                    context = ContextFactory.createContext("rocci");
                    context.setAttribute(Context.USERPROXY,
                            readRemoteProxy());
                    context.setAttribute(Context.USERID, "root");
                    context.setAttribute(Context.USERCERT,
                            getParamterValue("sshpublickey",
                                    Defaults.SSHPUBLICKEY));
                    context.setAttribute(Context.USERKEY,
                            getParamterValue("sshprivatekey",
                                    Defaults.SSHPRIVATEKEY));
                    break;
                case "unicore":
                case "ourgrid":
                case "bes-genesis2":
                case "gos":
                    throw new UnsupportedOperationException(
                            "Not supported yet.");
                case "ssh":
                    context = ContextFactory.createContext("UserPass");
                    if (params.get("username") != null) {
                        context.setAttribute("UserID",
                                params.get("username").getValue());
                    }
                    if (params.get("password") != null) {
                        context.setAttribute("UserPass",
                                params.get("password").getValue());
                    }
                    // This is a misterious setting coming from the
                    // legacy Grid Engine
                    context.setVectorAttribute("DataServiceAttributes",
                            new String[]{"sftp.KnownHosts="});
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Not supported yet.");
            }
            newSession.addContext(context);
        } catch (BadParameterException | IncorrectStateException
                | TimeoutException | DoesNotExistException
                | NotImplementedException | NoSuccessException ex) {
            log.error("Impossible to create a valid context for "
                    + infrastructure.getId());
            log.error(ex);
            throw new InfrastructureException("Impossible to open a session in"
                    + " the infratructure");
        } catch (AuthenticationFailedException | AuthorizationFailedException
                | PermissionDeniedException autherr) {
            log.error("Impossible to log-in to the infrastructure");
            log.error(autherr);
            throw new InfrastructureException("Impossible to open a session in"
                    + " the infratructure");
        }
        return newSession;
    }


    /**
     * Read the proxy certificate from a remote location.
     * The location is retrieved from the parameters.
     *
     * @return A string representation of the proxy
     * @throws InfrastructureException If the proxy for the infrastructure
     * cannot be retrieved for problems with the parameters
     */
    private String readRemoteProxy() throws InfrastructureException {
        URL proxy;
        if (getParamterValue("proxyurl", null) == null
                && getParamterValue("etokenserverurl", null) == null) {
            throw new InfrastructureException("No proxy location in "
                    + "configuration parameters for "
                    + infrastructure.getId());
        }

        if (getParamterValue("proxyurl", null) != null) {
            try {
                proxy = new URL(getParamterValue("proxyurl", null));
            } catch (MalformedURLException mue) {
                throw new InfrastructureException("URL for the proxy is not "
                        + "valid, infrastructure " + infrastructure.getId()
                        + " is not accessible");
            }
        } else {
            try {
                URI etokenurl = new URI(
                        getParamterValue("etokenserverurl", null));
                StringBuilder queryURI = new StringBuilder();
                StringBuilder pathURI = new StringBuilder();
                String oldPath = etokenurl.getPath();
                if (oldPath != null) {
                    pathURI.append(oldPath);
                    if (!oldPath.endsWith("/")) {
                        pathURI.append('/');
                    }
                    pathURI.append(getParamterValue("etokenid", ""));
                } else {
                    pathURI.append('/')
                            .append(getParamterValue("etokenid", ""));
                }
                String oldQuery = etokenurl.getQuery();
                if (oldQuery != null) {
                    queryURI.append(oldQuery).append('&');
                }
                queryURI.append("voms=")
                        .append(getParamterValue("vo", ""))
                        .append(':')
                        .append(getParamterValue("voroles", ""))
                        .append('&');
                queryURI.append("proxy-renewal=")
                        .append(getParamterValue("proxyrenewal",
                                Defaults.PROXYRENEWAL))
                        .append('&');
                queryURI.append("disable-voms-proxy=")
                        .append(getParamterValue("disablevomsproxy",
                                Defaults.DISABLEVOMSPROXY))
                        .append('&');
                queryURI.append("rfc-proxy=")
                        .append(getParamterValue("rfcproxy",
                                Defaults.RFCPROXY))
                        .append('&');
                queryURI.append("cn-label=");
                if (user != null) {
                    queryURI.append("eToken:").append(user);
                }
                etokenurl = new URI(
                        etokenurl.getScheme(),
                        etokenurl.getUserInfo(),
                        etokenurl.getHost(),
                        etokenurl.getPort(),
                        pathURI.toString(),
                        queryURI.toString(),
                        etokenurl.getFragment());
                proxy = etokenurl.toURL();
            } catch (MalformedURLException | URISyntaxException use) {
                throw new InfrastructureException("etokenserverurl not "
                        + "properly configured");
            }
        }
        StringBuilder strProxy = new StringBuilder();

        log.debug("Accessing the proxy " + proxy.toString());
        try {
            String lnProxy;
            BufferedReader fileProxy = new BufferedReader(
                    new InputStreamReader(proxy.openStream()));
            while ((lnProxy = fileProxy.readLine()) != null) {
                strProxy.append(lnProxy);
                strProxy.append("\n");
            }
        } catch (IOException ioer) {
            log.error("Impossible to retrieve the remote proxy certificate from"
                    + ": " + proxy.toString());
        }
        log.debug("Proxy:\n\n" + strProxy.toString() + "\n\n");
        return strProxy.toString();
    }



    /**
     * Retrieves parameter from the infrastructure.
     * @param name Parameter name
     * @param defaultValue Default value if the parameter is not defined
     * @return Parameter value
     */
    private String getParamterValue(final String name,
            final String defaultValue) {
        if (params.containsKey(name)) {
            return params.get(name).getValue();
        }
        return defaultValue;
    }
}
