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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;

import static retrofit2.Utils.methodError;

/**
 * Adapts an invocation of an interface method into an HTTP call.
 */
final class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
    /**
     * Inspects the annotations on an interface method to construct a reusable service method that
     * speaks HTTP. This requires potentially-expensive reflection so it is best to build each service
     * method only once and reuse it.
     * 检查接口方法上的注解，以构建可以说HTTP的可重用服务方法。
     * 这需要潜在的昂贵反射，因此最好只构建一次每个服务方法并重用它。
     */
    static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
            Retrofit retrofit, Method method, RequestFactory requestFactory) {
        // 获取一个 适配器，如果是RxJava的话，返回的就是RxJava适配器了
        CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter(retrofit, method);
        // 数据返回类型
        Type responseType = callAdapter.responseType();

        if (responseType == Response.class || responseType == okhttp3.Response.class) {
            throw methodError(method, "'"
                    + Utils.getRawType(responseType).getName()
                    + "' is not a valid response body type. Did you mean ResponseBody?");
        }
        // HEAD没有响应体
        if (requestFactory.httpMethod.equals("HEAD") && !Void.class.equals(responseType)) {
            throw methodError(method, "HEAD method must use Void as response type.");
        }

        //获取将返回数据序列化的转换器
        // 序列化和反序列化的转换器
        Converter<ResponseBody, ResponseT> responseConverter =
                createResponseConverter(retrofit, method, responseType);

        // 其实这个就是OkHttpClient
        okhttp3.Call.Factory callFactory = retrofit.callFactory;

        // 每一个请求方法就是一个对象
        return new HttpServiceMethod<>(requestFactory, callFactory, callAdapter, responseConverter);
    }

    private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
            Retrofit retrofit, Method method) {
        // 方法的返回类型
        Type returnType = method.getGenericReturnType();
        // 方法上面的注解
        Annotation[] annotations = method.getAnnotations();
        try {
            //noinspection unchecked
            // 获取相应的适配器，转换成不同的数据格式
            return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw methodError(method, e, "Unable to create call adapter for %s", returnType);
        }
    }

    /**
     * 创建返回值序列化的转换器
     *
     * @param retrofit
     * @param method
     * @param responseType
     * @param <ResponseT>
     * @return
     */
    private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
            Retrofit retrofit, Method method, Type responseType) {
        // 获取方法上面的注解
        Annotation[] annotations = method.getAnnotations();
        try {
            return retrofit.responseBodyConverter(responseType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw methodError(method, e, "Unable to create converter for %s", responseType);
        }
    }

    /**
     * 请求工程类，所有请求相关的配置都在这里
     * 比如请求方式啊，参数啊，请求头啊，请求体啊之类的
     */
    private final RequestFactory requestFactory;
    /**
     * 发起请求的工厂
     */
    private final okhttp3.Call.Factory callFactory;
    /**
     * 类型转换适配器
     */
    private final CallAdapter<ResponseT, ReturnT> callAdapter;
    /**
     * 序列化与反序列化的转换器
     */
    private final Converter<ResponseBody, ResponseT> responseConverter;

    private HttpServiceMethod(RequestFactory requestFactory, okhttp3.Call.Factory callFactory,
                              CallAdapter<ResponseT, ReturnT> callAdapter,
                              Converter<ResponseBody, ResponseT> responseConverter) {
        this.requestFactory = requestFactory;
        this.callFactory = callFactory;
        this.callAdapter = callAdapter;
        this.responseConverter = responseConverter;
    }

    @Override
    ReturnT invoke(Object[] args) {
        // 将call结果进行了 adapt
        return callAdapter.adapt(new OkHttpCall<>(requestFactory, args, callFactory, responseConverter));
    }
}
