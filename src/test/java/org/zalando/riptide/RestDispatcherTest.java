package org.zalando.riptide;

/*
 * ⁣​
 * riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Binding.consume;
import static org.zalando.riptide.Binding.map;
import static org.zalando.riptide.RestDispatcher.contentType;
import static org.zalando.riptide.RestDispatcher.from;
import static org.zalando.riptide.RestDispatcher.statusCode;

public class RestDispatcherTest {

    private static final ParameterizedTypeReference<Map<String, Number>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Number>>() {
            };

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    private final String url = "http://localhost/path";
    private final String textUrl = "http://localhost/path.txt";
    private final String jsonUrl = "http://localhost/path.json";

    private final RestTemplate template = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @Test
    public void shouldRejectDuplicateAttributeValues() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("application/json"));
        exception.expectMessage(containsString("application/xml"));
        
        from(template).dispatch(contentType(),
                consume(APPLICATION_JSON, Map.class, Object::toString),
                consume(APPLICATION_JSON, Map.class, Object::toString),
                consume(APPLICATION_XML, Map.class, Object::toString),
                consume(APPLICATION_XML, Map.class, Object::toString),
                consume(TEXT_PLAIN, String.class, Object::toString)
        );
    }

    @Test
    public void shouldConsumeTextPlain() {
        server.expect(requestTo(textUrl))
                .andRespond(withSuccess("It works!", TEXT_PLAIN));

        template.execute(textUrl, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, Object::toString),
                consume(APPLICATION_JSON, Map.class, m -> {
                    throw new AssertionError("Didn't expect json");
                })
        ));
    }

    @Test
    public void shouldConsumeApplicationJson() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        template.execute(jsonUrl, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, s -> {
                    throw new AssertionError("Didn't expect text");
                }),
                consume(APPLICATION_JSON, Map.class, Object::toString)
        ));
    }

    @Test
    public void shouldMapTextPlain() {
        server.expect(requestTo(textUrl))
                .andRespond(withSuccess("It works!", TEXT_PLAIN));

        final ResponseEntity<String> response = template.execute(textUrl, GET, null, from(template).dispatch(contentType(),
                map(TEXT_PLAIN, String.class, Object::toString),
                map(APPLICATION_JSON, Map.class, Object::toString)
        ));

        assertThat(response.getBody(), is("It works!"));
    }

    @Test
    public void shouldMapApplicationJson() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        final ResponseEntity<String> response = template.execute(jsonUrl, GET, null, from(template).dispatch(contentType(),
                map(TEXT_PLAIN, String.class, Object::toString),
                map(APPLICATION_JSON, Map.class, Object::toString)
        ));

        assertThat(response.getBody(), is("{}"));
    }

    @Test
    public void shouldConsumerOk() {
        server.expect(requestTo(url)).andRespond(withSuccess().body("It works!"));

        template.execute(url, GET, null, from(template).dispatch(statusCode(),
                consume(HttpStatus.OK, String.class, Object::toString),
                consume(HttpStatus.NOT_FOUND, String.class, s -> {
                    throw new AssertionError("Didn't expect 404");
                })
        ));
    }

    @Test
    public void shouldConsumeNotFound() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not found"));

        template.setErrorHandler(new PassThroughResponseErrorHandler());
        template.execute(url, GET, null, from(template).dispatch(statusCode(),
                consume(HttpStatus.OK, String.class, s -> {
                    throw new AssertionError("Didn't expect 200");
                }),
                consume(HttpStatus.NOT_FOUND, String.class, Object::toString
                )
        ));
    }

    @Test(expected = RestClientException.class)
    public void shouldFailIfNoMatch() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        template.execute(jsonUrl, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, Object::toString),
                consume(APPLICATION_XML, String.class, Object::toString)
        ));
    }

    @Test
    public void shouldConsumeParameterizedType() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{\"value\":123}", APPLICATION_JSON));

        final AtomicReference<Map<String, Number>> ref = new AtomicReference<>();

        template.execute(jsonUrl, GET, null, from(template).dispatch(contentType(),
                consume(TEXT_PLAIN, String.class, Object::toString),
                consume(APPLICATION_JSON, MAP_TYPE, ref::set)
        ));

        assertThat(ref.get(), hasEntry("value", 123));
    }

    @Test
    public void shouldMapParameterizedType() {
        server.expect(requestTo(jsonUrl))
                .andRespond(withSuccess("{\"value\":123}", APPLICATION_JSON));

        final ResponseEntity<Number> entity = template.execute(jsonUrl, GET, null,
                from(template).dispatch(contentType(),
                        map(TEXT_PLAIN, String.class, s -> 0),
                        map(APPLICATION_JSON, MAP_TYPE, m -> m.get("value"))
                ));
        
        assertThat(entity.getBody(), is(123));
    }

}