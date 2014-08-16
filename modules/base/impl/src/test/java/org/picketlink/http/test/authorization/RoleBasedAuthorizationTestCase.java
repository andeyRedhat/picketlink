/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.http.test.authorization;

import org.junit.Test;
import org.picketlink.annotations.PicketLink;
import org.picketlink.config.SecurityConfigurationBuilder;
import org.picketlink.event.SecurityConfigurationEvent;
import org.picketlink.http.internal.schemes.FormAuthenticationScheme;
import org.picketlink.http.test.AbstractSecurityFilterTestCase;
import org.picketlink.test.weld.Deployment;
import org.picketlink.http.test.SecurityInitializer;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Pedro Igor
 */
@Deployment(
    beans = {
        RoleBasedAuthorizationTestCase.SecurityConfiguration.class, SecurityInitializer.class
    },
    excludeBeansFromPackage = "org.picketlink.http.test"
)
public class RoleBasedAuthorizationTestCase extends AbstractSecurityFilterTestCase {

    @Inject
    @PicketLink
    private Instance<HttpServletRequest> picketLinkRequest;

    @Test
    public void testOnlyManagers() throws Exception {
        when(this.request.getServletPath()).thenReturn("/formProtectedUri/" + FormAuthenticationScheme.J_SECURITY_CHECK);
        when(this.request.getParameter(FormAuthenticationScheme.J_USERNAME)).thenReturn("picketlink");
        when(this.request.getParameter(FormAuthenticationScheme.J_PASSWORD)).thenReturn("picketlink");

        this.securityFilter.doFilter(this.request, this.response, this.filterChain);
        verify(this.filterChain, times(0)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(this.response).sendRedirect(CONTEXT_PATH);

        when(this.request.getServletPath()).thenReturn("/onlyManagerRole");
        reset(this.response);

        this.securityFilter.doFilter(this.request, this.response, this.filterChain);

        assertEquals("/onlyManagerRole", picketLinkRequest.get().getServletPath());
    }

    @Test
    public void testRoleNotAllowed() throws Exception {
        when(this.request.getServletPath()).thenReturn("/formProtectedUri/" + FormAuthenticationScheme.J_SECURITY_CHECK);
        when(this.request.getParameter(FormAuthenticationScheme.J_USERNAME)).thenReturn("picketlink");
        when(this.request.getParameter(FormAuthenticationScheme.J_PASSWORD)).thenReturn("picketlink");

        this.securityFilter.doFilter(this.request, this.response, this.filterChain);
        verify(this.filterChain, times(0)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(this.response).sendRedirect(CONTEXT_PATH);

        when(this.request.getServletPath()).thenReturn("/onlyCustomerRole");
        reset(this.response);

        this.securityFilter.doFilter(this.request, this.response, this.filterChain);

        verify(this.filterChain, times(0)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(this.response, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    public static class SecurityConfiguration {
        public void configureHttpSecurity(@Observes SecurityConfigurationEvent event) {
            SecurityConfigurationBuilder builder = event.getBuilder();
            builder
                .http()
                .allPaths()
                .inbound()
                .authc()
                .form()
                .path("/onlyManagerRole")
                .inbound()
                .authz()
                .allowedRoles("Manager")
                .path("/onlyCustomerRole")
                .inbound()
                .authz()
                .allowedRoles("Customer");
        }
    }
}
