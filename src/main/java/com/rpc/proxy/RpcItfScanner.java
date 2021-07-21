package com.rpc.proxy;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;



import java.io.IOException;
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


    // remote scan interfaces(only information) and ServerInfo

    /**
     * scan way;
     * can be changed with other ways such as by service's full name
     *
     * @param basePackages redis provided
     * @return
     */
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        addFilter();
        // scan -->  BD holders --> BDs  -->set some parameters
        Set<BeanDefinitionHolder> holders = super.doScan(basePackages);
        if(holders != null && holders.size() > 0){
            for(BeanDefinitionHolder holder : holders){
                GenericBeanDefinition beanDefinition = (GenericBeanDefinition)holder.getBeanDefinition();
                // constructor argument values
                // add a generic argument value to be matched by name.
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition.getBeanClassName());
                // since instantiation needs to select Constructor !!
                beanDefinition.setBeanClass(RpcInterfaceProxyFactoryBean.class);  //factory bean
                // beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            }
        }
        return holders;
    }

    /**
     * for rpc, open interface(api) for a certain service is must;
     *
     * @param beanDefinition
     * @return
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata(); //get metadata of this BD
        // notice here
        return metadata.hasAnnotation("com.rpc.annotations.RpcReference") &&
                metadata.isInterface() && metadata.isIndependent();  //not dependent on an enclosing class
        // default: (metadata.isIndependent() && (metadata.isConcrete()||(metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))));
    }

    private void addFilter(){
        addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    }

}
