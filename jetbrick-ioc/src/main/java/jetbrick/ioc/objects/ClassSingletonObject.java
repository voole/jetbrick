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

import java.lang.reflect.Method;
import java.util.List;
import jetbrick.io.config.Configuration;
import jetbrick.ioc.Ioc;
import jetbrick.ioc.injectors.*;
import jetbrick.reflect.KlassInfo;

public class ClassSingletonObject extends SingletonObject {
    protected final Class<?> beanClass;
    private Configuration properties;

    public ClassSingletonObject(Ioc ioc, Class<?> beanClass, Configuration properties) {
        super(ioc);
        this.beanClass = beanClass;
        this.properties = properties;
    }

    @Override
    protected Object doGetObject() throws Exception {
        KlassInfo klass = KlassInfo.create(beanClass);
        CtorInjector ctorInjector = IocObjectUtils.doGetCtorInjector(ioc, klass);
        List<FieldInjector> fieldInjectors = IocObjectUtils.doGetFieldInjectors(ioc, klass);
        List<PropertyInjector> propertyInjectors = IocObjectUtils.doGetPropertyInjectors(ioc, klass, properties);
        Method initializeMethod = IocObjectUtils.doGetInitializeMethod(klass);

        properties = null;

        Object object;
        if (ctorInjector == null) {
            object = beanClass.newInstance();
        } else {
            object = ctorInjector.newInstance();
        }

        for (PropertyInjector injector : propertyInjectors) {
            injector.set(object);
        }
        for (FieldInjector injector : fieldInjectors) {
            injector.set(object);
        }

        if (initializeMethod != null) {
            initializeMethod.invoke(object, (Object[]) null);
        }

        return object;
    }
}
