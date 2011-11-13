/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jibx.examples.cxf.ws.impl;

import javax.jws.WebService;
import org.jibx.examples.cxf.ws.*;
import org.jibx.examples.cxf.ws.types.*;

@WebService(serviceName = "PersonService", targetNamespace = "http://jibx.org/examples/cxf/ws", endpointInterface = "org.jibx.examples.cxf.ws.Person")
public class PersonImpl implements Person {

	public GetPersonResponse getPerson(GetPerson person) {
        if (person.getPersonId() == null || person.getPersonId().length() == 0) {

        }
        GetPersonResponse response = new GetPersonResponse();

        response.setPersonId(person.getPersonId());
        response.setName("Guillaume");
        response.setSsn("000-000-0000");
		
        return response;
	}

}
