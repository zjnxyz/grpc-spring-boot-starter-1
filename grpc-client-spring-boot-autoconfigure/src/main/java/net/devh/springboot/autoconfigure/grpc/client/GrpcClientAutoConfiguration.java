package net.devh.springboot.autoconfigure.grpc.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.LoadBalancer;
import io.grpc.util.RoundRobinLoadBalancerFactory;

/**
 * User: Michael
 * Email: yidongnan@gmail.com
 * Date: 5/17/16
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass({GrpcChannelFactory.class})
public class GrpcClientAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public GrpcChannelsProperties grpcChannelsProperties() {
        return new GrpcChannelsProperties();
    }

    @Bean
    public GlobalClientInterceptorRegistry globalClientInterceptorRegistry() {
        return new GlobalClientInterceptorRegistry();
    }

    @ConditionalOnMissingBean
    @Bean
    public LoadBalancer.Factory grpcLoadBalancerFactory() {
        return RoundRobinLoadBalancerFactory.getInstance();
    }

    @ConditionalOnMissingBean(value = GrpcChannelFactory.class, type = "org.springframework.cloud.client.discovery.DiscoveryClient")
    @Bean
    public GrpcChannelFactory addressChannelFactory(GrpcChannelsProperties channels, LoadBalancer.Factory loadBalancerFactory) {
        return new AddressChannelFactory(channels, loadBalancerFactory);
    }

    @Bean
    @ConditionalOnClass(GrpcClient.class)
    public GrpcClientBeanPostProcessor grpcClientBeanPostProcessor() {
        return new GrpcClientBeanPostProcessor();
    }

    @Configuration
    @ConditionalOnBean(DiscoveryClient.class)
    protected static class DiscoveryGrpcClientAutoConfiguration {

        @ConditionalOnMissingBean
        @Bean
        public GrpcChannelFactory discoveryClientChannelFactory(GrpcChannelsProperties channels, DiscoveryClient discoveryClient, LoadBalancer.Factory loadBalancerFactory) {
            return new DiscoveryClientChannelFactory(channels, discoveryClient, loadBalancerFactory);
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "spring.sleuth.scheduled.enabled", matchIfMissing = true)
    @ConditionalOnClass(Tracer.class)
    protected static class TraceClientAutoConfiguration {

        @Bean
        public GlobalClientInterceptorConfigurerAdapter globalTraceClientInterceptorConfigurerAdapter(Tracer tracer) {
            return new GlobalClientInterceptorConfigurerAdapter() {

                @Override
                public void addClientInterceptors(GlobalClientInterceptorRegistry registry) {
                    registry.addClientInterceptors(new TraceClientInterceptor(tracer, new MetadataInjector()));
                }
            };
        }
    }
}
