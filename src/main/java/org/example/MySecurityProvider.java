package org.example;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;
import io.undertow.util.StatusCodes;
import org.apache.camel.component.undertow.spi.UndertowSecurityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


public class MySecurityProvider implements UndertowSecurityProvider {
    public static final String PRINCIPAL_NAME_HEADER = MySecurityProvider.class.getName() + "_principal";
    private static final Logger LOG = LoggerFactory.getLogger(MySecurityProvider.class);
    private static final AttachmentKey<String> PRINCIPAL_NAME_KEY = AttachmentKey.create(String.class);

    private Filter securityFilter;

    private Map<Undertow, DeploymentManager> deploymenMap = new LinkedHashMap<>();


    @Override
    public void addHeader(BiConsumer<String, Object> consumer, HttpServerExchange httpExchange) throws Exception {
        String principalName = httpExchange.getAttachment(PRINCIPAL_NAME_KEY);
        consumer.accept(PRINCIPAL_NAME_HEADER, principalName);
    }

    @Override
    public int authenticate(HttpServerExchange httpExchange, List<String> allowedRoles) throws Exception {
        ServletRequestContext servletRequestContext = httpExchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        ServletRequest request = servletRequestContext.getServletRequest();
        ServletResponse response = servletRequestContext.getServletResponse();

        //new filter has to be added into the filter chain. If is successfully called it means that security allows access.
        FilterChain fc = (servletRequest, servletResponse) -> {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a instanceof UsernamePasswordAuthenticationToken) {
                LOG.debug("Authentication token is present.");
                boolean allowed = false;
                Collection<GrantedAuthority> grantedAuthorities = (Collection<GrantedAuthority>) a.getAuthorities();
                for (GrantedAuthority grantedAuthority : grantedAuthorities) {
                    if (allowedRoles.contains(grantedAuthority.getAuthority())) {
                        LOG.debug("Authenticated principal {} has authority to access resource.", a.getName());
                        allowed = true;
                        break;
                    }
                }

                if (allowed) {
                    httpExchange.putAttachment(PRINCIPAL_NAME_KEY, a.getName());
                    httpExchange.setStatusCode(StatusCodes.OK);
                    return;
                } else {
                    LOG.debug("Authenticated principal {} doesn't have authority to access resource.", a.getName());
                }

            } else {
                //this is logged as warn, because it shows an error in configuration
                //spring-security shouldn't allow to access this code if configuration is correct
                LOG.warn("Authentication token is not present. Access is FORBIDDEN.");
            }
            httpExchange.setStatusCode(StatusCodes.FORBIDDEN);
        };
        securityFilter.doFilter(request, response, fc);

        return httpExchange.getStatusCode();
    }

    @Override
    public boolean acceptConfiguration(Object configuration, String endpointUri) throws Exception {
            if(configuration != null){
            this.securityFilter = ((AbstractFilterRegistrationBean)configuration).getFilter();
            return true;
        }

        return false;
    }

    @Override
    public Undertow registerHandler(Undertow.Builder builder, HttpHandler handler) throws Exception {
        DeploymentInfo deployment = Servlets.deployment()
                .setContextPath("")
                .setDisplayName("application")
                .setDeploymentName("camel-undertow")
                .setClassLoader(getClass().getClassLoader())
                //httpHandler for servlet is ignored, camel handler is used instead of it
                .addOuterHandlerChainWrapper(h -> handler);

        DeploymentManager deploymentManager = Servlets.newContainer().addDeployment(deployment);
        deploymentManager.deploy();
        Undertow undertow = UndertowSecurityProvider.super.registerHandler(builder, deploymentManager.start());
        //save into cache for future unregistration
        deploymenMap.put(undertow, deploymentManager);
        return undertow;
    }

    @Override
    public void unregisterHandler(Undertow undertow) {
        if (deploymenMap.containsKey(undertow)) {
            deploymenMap.get(undertow).undeploy();
            deploymenMap.remove(undertow);
        }
    }
}
