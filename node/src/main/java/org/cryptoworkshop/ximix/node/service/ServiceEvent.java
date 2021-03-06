/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.node.service;

/**
 * Carrier class to link service event types with parameters.
 */
public class ServiceEvent
{
    public static enum Type
    {
        PUBLISH_STATISTICS
    }

    private final Type type;
    private final Object parameter;

    /**
     * Base constructor.
     *
     * @param type the type of the service event.
     * @param parameter the parameters required to handle the event.
     */
    public ServiceEvent(Type type, Object parameter)
    {
        this.type = type;
        this.parameter = parameter;
    }

    public Type getType()
    {
        return type;
    }

    public Object getParameter()
    {
        return parameter;
    }
}
