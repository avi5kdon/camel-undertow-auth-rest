/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.example;

import org.apache.camel.component.undertow.RestUndertowHttpBinding;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.UndertowHttpBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

/**
 * A spring-boot application that includes a Camel route builder to setup the Camel routes
 */
@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application  {

    @Autowired
    private DelegatingFilterProxyRegistrationBean delegatingFilterProxyRegistrationBean;

    public static void main(String[] args) throws Exception{
    SpringApplication.run(Application.class, args);
    }

    @Bean(name = "undertow")
    public UndertowComponent undertowComponent(){
        UndertowComponent undertowComponent = new UndertowComponent();
        undertowComponent.setAllowedRoles("admin,guest");
        undertowComponent.setSecurityConfiguration(delegatingFilterProxyRegistrationBean);
        return undertowComponent;
    }



}
