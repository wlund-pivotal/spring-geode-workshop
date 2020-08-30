/*
 * Copyright 2020 the original author or authors.
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
package com.vmware.spring.geode.workshop.crm.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.mapping.annotation.Region;

/**
 * An Abstract Data Type (ADT) modeling a {@literal Customer}.
 *
 * @author John Blum
 * @see org.springframework.data.annotation.Id
 * @see org.springframework.data.gemfire.mapping.annotation.Region
 * @since 1.0.0
 */
// tag::class[]
@Region("Customers")
public class Customer<T> {

	@Id
	private Long id;
	private String name;

	private Customer(Long id, String name) {
		if ((id == null) ||name == null) throw new NullPointerException("name required");
		this.id = id;
		this.name = (String) name;
	}
	
	public static <T> Customer<T> newCustomer(Long id, String name) {
	     return new Customer<T>(id, name);
	    }
	
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
	@Override
	public String toString() {
		return "Customer(" + this.id + ", " + this.name + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Customer other = (Customer) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
}
// end::class[]
