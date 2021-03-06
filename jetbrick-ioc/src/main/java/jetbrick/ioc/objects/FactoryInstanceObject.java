/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 * Email: subchen@gmail.com
 * URL: http://subchen.github.io/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.ioc.objects;

import jetbrick.io.config.Configuration;
import jetbrick.ioc.Ioc;
import jetbrick.ioc.IocFactory;

public class FactoryInstanceObject extends InstanceObject {
    private final Class<?> factoryClass;
    private IocFactory<?> factory;
    private Configuration properties;

    public FactoryInstanceObject(Ioc ioc, Class<?> factoryClass, Configuration properties) {
        super(ioc);
        this.factoryClass = factoryClass;
        this.properties = properties;
    }

    @Override
    protected void initialize() throws Exception {
        IocObject factoryCreator = new ClassSingletonObject(ioc, factoryClass, properties);
        factory = (IocFactory<?>) factoryCreator.getObject();
        properties = null;
    }

    @Override
    protected Object doGetObject() throws Exception {
        return factory.getObject();
    }
}
