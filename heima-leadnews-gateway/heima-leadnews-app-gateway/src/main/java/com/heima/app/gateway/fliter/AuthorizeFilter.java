package com.heima.app.gateway.fliter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.heima.app.gateway.uils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AuthorizeFilter implements Ordered, GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取request response对象
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        //判断是否是登录请求
        if (request.getURI().getPath().contains("/login")) {
            return chain.filter(exchange);
        }
        //判断是否有token
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //判断token是否有效
        int result = 0;
        try {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            result = AppJwtUtil.verifyToken(claimsBody);
            if (result == 1 || result == 2) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        if (result == -1) {
            return handleResponse(exchange,chain,token);
        }
        // 放行
        return chain.filter(exchange);
    }

    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain,String token) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        // probably should reuse buffers
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        //释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        String s = new String(content, Charset.forName("UTF-8"));
                        String lastStr = s;
                        //TODO，s就是response的值，想修改、查看就随意而为了
                        JSONObject jsonObject = JSONUtil.parseObj(lastStr);
                        if(jsonObject.containsKey("data")){
                            String data = jsonObject.getStr("data");
                            JSONObject dataObj = JSONUtil.parseObj(data);
                            if(!dataObj.containsKey("token")){
                                Integer id = (Integer)AppJwtUtil.getClaimsBody(token).get("id");
                                dataObj.set("token",AppJwtUtil.getToken(id.longValue()));
                            }
                            String dataString = dataObj.toStringPretty();
                            jsonObject.set("data",dataString);
                            lastStr = jsonObject.toStringPretty();
                        }else{
                            Map<String, String> map = new HashMap<>();
                            Integer id = (Integer)AppJwtUtil.getClaimsBody(token).get("id");
                            map.put("token",AppJwtUtil.getToken(id.longValue()));
                            jsonObject.set("data",map);
                            lastStr = jsonObject.toStringPretty();
                        }
                        byte[] uppedContent = lastStr.getBytes();
                        originalResponse.getHeaders().setContentLength(uppedContent.length);
                        return bufferFactory.wrap(uppedContent);
                    }));
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
        // replace response with decorator
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }
    @Override
    public int getOrder() {
        return -2;
    }
}
