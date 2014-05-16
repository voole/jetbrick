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
package jetbrick.web.mvc.action.annotations;

import jetbrick.commons.typecast.Convertor;
import jetbrick.ioc.annotations.Managed;
import jetbrick.web.mvc.RequestContext;
import jetbrick.web.mvc.action.ArgumentGetterResolver;

@Managed
public class RequestHeaderArgumentGetter implements AnnotatedArgumentGetter<RequestHeader, Object> {
    private String name;
    private boolean required;
    private String defaultValue;
    private Convertor<?> typeConvertor;

    @Override
    public void initialize(Class<?> type, RequestHeader annotation) {
        name = annotation.value();
        required = annotation.required();
        defaultValue = ValueConstants.defaultIfNull(annotation.defaultValue());
        typeConvertor = ArgumentGetterResolver.getTypeConvertor(type);
    }

    @Override
    public Object get(RequestContext ctx) {
        String value = ctx.getHeader(name);
        if (value == null) {
            value = defaultValue;
        }
        if (value == null) {
            if (required) {
                throw new IllegalStateException();
            }
            return null;
        }
        if (typeConvertor != null) {
            return typeConvertor.convert(value);
        }
        return value;

    }
}