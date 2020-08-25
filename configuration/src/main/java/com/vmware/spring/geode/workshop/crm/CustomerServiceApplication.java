/*
list * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.vmware.spring.geode.workshop.crm;


import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.geode.config.annotation.EnableClusterAware;

import com.vmware.spring.geode.workshop.crm.model.Customer;
import com.vmware.spring.geode.workshop.crm.repo.CustomerRepository;

import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;

/**
 * Spring Boot application implementing a Customer Service.
 *
 * @author John Blum
 * @see org.springframework.boot.ApplicationRunner
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.boot.builder.SpringApplicationBuilder
 * @see org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions
 * @since 1.0.0
 */
// tag::class[]
/***/
@SpringBootApplication
@EnableClusterAware
@EnableEntityDefinedRegions(basePackageClasses = Customer.class)

/**
 * 
 * @author wlund
 * Without AutoConfiguration
@SpringBootApplication(exclude = ClientCacheAutoConfiguration.class)
@EnableEntityDefinedRegions(basePackageClasses = Customer.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
/**Disabling Spring Data Repositories Auto-configuration.**
 @SpringBootApplication(exclude = RepositoriesAutoConfiguration.class)
@EnableEntityDefinedRegions(basePackageClasses = Customer.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
*/
public class CustomerServiceApplication {

	public static void main(String[] args) {

		new SpringApplicationBuilder(CustomerServiceApplication.class)
			.web(WebApplicationType.NONE)
			.build()
			.run(args);
	}

	@Bean
	ApplicationRunner runner(CustomerRepository customerRepository) {
		
		Fairy fairy = Fairy.create();

		// Locating the max id to be enable incrementing of our ID. 
		Long id = customerRepository.count();
		
		// Generate a random name from data generator
		Person person = fairy.person();

		return args -> {

			assertThat(customerRepository.count()).isEqualTo(id);

			Customer jonDoe = Customer.newCustomer(id + 1L, person.fullName());

			System.err.printf("Saving Customer [%s]%n", jonDoe);

			jonDoe = customerRepository.save(jonDoe);

			assertThat(jonDoe).isNotNull();
			assertThat(jonDoe.getId()).isEqualTo(id + 1);
			assertThat(jonDoe.getName()).isEqualTo(person.fullName());
			assertThat(customerRepository.count()).isEqualTo(jonDoe.getId());

			String query = "Querying for Customer [SELECT * FROM /Customers WHERE name LIKE " + jonDoe.getName() + "]";
			System.err.println(query);

			Customer queriedJonDoe = customerRepository.findByNameLike(person.fullName());

			assertThat(queriedJonDoe).isEqualTo(jonDoe);

			System.err.printf("Customer was [%s]%n", queriedJonDoe);
		};
	}
}
// end::class[]
