package com.rpc.consumer.proxy;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;


import java.util.Set;

/**
 * @user KyZhang
 * @date
 */
public class RpcItfScanner extends ClassPathBeanDefinitionScanner
{

    public RpcItfScanner(BeanDefinitionRegistry registry) {
        super(registry,false);
    }


    /**
     * scan way;
     * can be changed with other ways such as by service's full name
     *
     * @param basePackages  the package in which the remote service interface lies
     * @return
     */
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        addFilter();
        // scan -->  BD holders --> BDs  -->set some parameters
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        if(holders != null && holders.size() > 0){
            for(BeanDefinitionHolder holder : holders){
                GenericBeanDefinition beanDefinition = (GenericBeanDefinition)holder.getBeanDefinition();
                /*
                 * constructor argument values;
                 * add a generic argument value to be matched by name.
                 */
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition.getBeanClassName());
                // since instantiation needs to select constructor!
                beanDefinition.setBeanClass(RpcInterfaceProxyFactoryBean.class);  //factory bean
            }
        }
        return holders;
    }


    /**
     * for rpc, open interface(API) for a certain service is must
     *
     * @param beanDefinition
     * @return
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata(); //get metadata of this BD
        /*
         * default: (metadata.isIndependent() && (metadata.isConcrete()||
         * (metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))));
         */
        return metadata.hasAnnotation("com.rpc.annotation.RpcReference") &&
                metadata.isInterface() && metadata.isIndependent();  //not dependent on an enclosing class
    }

    private void addFilter(){
        addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    }

}
