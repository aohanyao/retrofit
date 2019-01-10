/*
 * Copyright (C) 2015 Square, Inc.
 *
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
 */
package retrofit2;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import static retrofit2.Utils.methodError;

// 抽象类，那就是有实现类咯
abstract class ServiceMethod<T> {

    static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
        // 请求工厂 解析注解
        RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);
        // 获取方法的返回类型
        Type returnType = method.getGenericReturnType();
        // 判断是否是我当前无法处理的返回类型
        if (Utils.hasUnresolvableType(returnType)) {
            // 抛出一个方法异常
            // TODO 这里就可以推断出Retrofit所支持的类型
            throw methodError(method,
                    "Method return type must not include a type variable or wildcard: %s", returnType);
        }
        if (returnType == void.class) {
            throw methodError(method, "Service methods cannot return void.");
        }
        // 对请求服务的接口中的注解进行解析
        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }


    abstract T invoke(Object[] args);
}
