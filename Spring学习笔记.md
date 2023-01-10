> Spring Framework Documentation: https://docs.spring.io/spring-framework/docs/current/reference/html/index.html

[TOC]

# 1. IoC容器

## 1.1 IoC容器和Beans介绍

`BeanFactory`接口使用配置来管理任何类型的对象，`ApplicationContext`是其子接口，相比之下新增以下功能：

- 集成Spring AOP功能
- 事件发布（Event Publication）
- 应用层特定容器，例如`WebApplicationContext`应用于web应用

## 1.2 容器简介

### 1.2.2 初始化容器

通过XML配置来实例化Spring容器，典型的配置文件如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- services -->
    <bean id="petStore" class="org.springframework.samples.jpetstore.services.PetStoreServiceImpl">
        <property name="accountDao" ref="accountDao"/>
        <property name="itemDao" ref="itemDao"/>
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>
  
    <!--daos-->
    <bean id="accountDao"
        class="org.springframework.samples.jpetstore.dao.jpa.JpaAccountDao">
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>
    <bean id="itemDao" class="org.springframework.samples.jpetstore.dao.jpa.JpaItemDao">
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>

    <import resource="services.xml"/>
    <import resource="resources/messageSource.xml"/>
    <import resource="/resources/themeSource.xml"/>
</beans>
```

- 其中可以通过`<import/>`标签引入其他XML配置文件的定义，路径都是相对路径，必须在当前文件的目录或子目录下。**开头的斜杠`/`会被忽略掉**，也即`/resources/themeSource.xml`的`themeSource.xml`必须在当前文件的子目录下
- 可以使用相对路径`../`来引用父目录中的配置文件，但是不推荐
- 可以使用完整路径：`file:C:/config/services.xml`或者`classpath:/config/services.xml`，但是会导致服务应用跟特定的绝对路径耦合

### 1.2.3 使用容器

`ApplicationContext`注册并维护beans及其依赖，可以通过其接口`T getBean(String name, Class<T> requiredType)`获取beans

```java
// create and configure beans
ApplicationContext context = new ClassPathXmlApplicationContext("services.xml", "daos.xml");

// retrieve configured instance
PetStoreService service = context.getBean("petStore", PetStoreService.class);

// use configured instance
List<String> userList = service.getUsernameList();
```

有一种更加灵活的方式是使用`GenericApplicationContext`，并结合代理来读取配置文件，比如`XmlBeanDefinitionReader`

```java
GenericApplicationContext context = new GenericApplicationContext();
// 使用XmlBeanDefinitionReader来读取配置文件
new XmlBeanDefinitionReader(context).loadBeanDefinitions("services.xml", "daos.xml");
context.refresh();
```

**原则**：在项目应用中，不应该显式地使用Spring API来获取beans，而应该使用依赖注入，通过元数据（配置文件或注解）来声明依赖

## 1.3 Bean简介

容器内，bean定义被保存到`BeanDefinition`对象内，其中包含以下元数据：

- 包限定类名：通常是bean的实际类型
- Bean的行为：比如scope、lifecycle callback等等在容器中的行为
- 对其他bean的依赖（称为：Reference引用 or Collaborator合作者 or Dependency依赖）
- 其他配置参数，如连接池大小等等

除了通过元数据来定义bean之外，还支持通过Spring API将在容器之外创建的现有对象注册到容器中

```java
ApplicationContext context = ...;
DefaultListableBeanFactory beanFactory = context.getBeanFactory();
// 注册外部对象
beanFactory.registerSingleton(...);
beanFactory.registerBeanDefinition(...);
```

**原则**：使用bean注册的API可能存在覆盖原有bean定义和实例的风险，并且在运行时注册新的bean存在并发风险，是不推荐的使用做法

### 1.3.1 命名Bean

**标识符**（Identifier）

每个bean都可以有一个及以上的标识符，标识符在容器内必须全局唯一。通常只会有一个，如果存在多个以上，可以看做是别名。

在XML配置文件中，可以使用`id`属性或`name`属性或两者同时使用来标识一个bean，属性值通常为字母数字，但也可包含特殊字符。

`id`属性和`name`属性中指定的标识符都必须是全局唯一的，两个属性的作用是等价的。

**别名**：`id`属性可以唯一标识一个bean，如果需要别名，**可以在`name`属性中列出多个别名**（`id`属性里只能一个），并用逗号`,`分号 `;` 或空格隔开。可以想到，这一块逻辑应该可以在`XmlBeanDefinitionReader`等bean定义的解析类中看到。

**默认标识符**：对于未命名的bean，Spring会自动为其生成一个`name`，默认为类名+首字母小写。如果类名前两个字母全为大写，则不会小写首字母。多说无益，看代码`java.beans.Introspector`

```java
    /**
     * Utility method to take a string and convert it to normal Java variable
     * name capitalization.  This normally means converting the first
     * character from upper case to lower case, but in the (unusual) special
     * case when there is more than one character and both the first and
     * second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
     * as "URL".
     */
    public static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
```

**别名配置**：如果需要使用其他地方定义的bean name，可以使用别名配置，一个bean的name为`fromName`，同时也可以使用`toName`来引用

```xml
<alias name="fromName" alias="toName"/>
```

### 1.3.2 初始化Bean

在`<bean/>`标签的`class`属性中，通常需要强制指定bean的类型，该类型可以是bean的实际类型，也可以是一个工厂，通过调用其方法来创建一个bean，方法的返回值类型就是bean的类型。

如果`class`需要指定一个内部类，可以使用`$`或者常规的`.`符号来分割即可。如类`com.example.AbClass`有一个静态内部类`XyClass`，则`class`属性可以填写`com.example.AbClass&XyClass`或者`com.example.AbClass.XyClass`

指定的类无需实现任何特定的接口或者遵循任何特定的格式，只需指定一个任意的类即可，但是对于不同的IoC容器，可能要求类包含一个无参的构造方法

#### 使用静态工厂方法

如果需要使用静态工厂方法来创造bean，可以使用`factory-method`属性来指定要使用的工厂方法（**必须是静态方法**），`class`需要指定的是包含工厂方法的类，而不是工厂方法创造的对象类

```xml
<!--ClientService.createInstance()工厂方法-->
<bean id="clientService" class="examples.ClientService" factory-method="createInstance"/>
```

#### 使用实例工厂方法

除了使用静态工厂方法外，还可以使用实例方法来创建bean。在`factory-bean`属性中指定容器中已有的bean，并在`factory-method`属性中指定该bean的一个实例方法，用以创建另一个bean。工厂bean本身可以由容器来管理（进行依赖注入），并且该工厂bean可以包含多个实例方法用以创建其他bean。

```xml
<!-- the factory bean, which contains a method called createInstance() -->
<bean id="serviceLocator" class="examples.DefaultServiceLocator">
    <!-- inject any dependencies required by this locator bean -->
</bean>

<!-- the bean to be created via the factory bean -->
<bean id="clientService" factory-bean="serviceLocator" factory-method="createClientServiceInstance"/>
```

```java
public class DefaultServiceLocator {
    private static ClientService clientService = new ClientServiceImpl();

    public ClientService createClientServiceInstance() {
        return clientService;
    }
}
```

**确定Bean的运行时类型**

可以通过`BeanFactory.getType(String beanName)`来获取bean的运行时类型（或者说实际类型），返回的即是`BeanFactory.getBean(String beanName)`方法获取的bean的实际类型

## 1.4 依赖

### 1.4.1 依赖注入

> Dependency injection (DI) is a process whereby objects define their dependencies (that is, the other objects with which they work) only through constructor arguments, arguments to a factory method, or properties that are set on the object instance after it is constructed or returned from a factory method. The container then injects those dependencies when it creates the bean. This process is fundamentally the inverse (hence the name, Inversion of Control) of the bean itself controlling the instantiation or location of its dependencies on its own by using direct construction of classes or the Service Locator pattern.

**依赖注入**： 对象通过构造方法参数、工厂方法参数或者setter方法参数来声明其依赖，并凭借容器为其注入，而不是由对象自己直接构造并控制其依赖（控制反转）

好处：1）构造和使用解耦，无需关注依赖的构造、位置、具体类；2）便于测试，当依赖接口或抽象类时，可以使用不同实现或者mock实现来进行测试

#### 构造方法注入

**类型匹配**：默认按照参数类型匹配。在不存在歧义的情况下，会简单按照bean定义的参数顺序来构造bean

```java
package x.y;

public class ThingOne {

    public ThingOne(ThingTwo thingTwo, ThingThree thingThree) {
        // ...
    }
}
```

```xml
<beans>
    <bean id="beanOne" class="x.y.ThingOne">
        <!--默认按照定义的顺序-->
        <constructor-arg ref="beanTwo"/>
        <constructor-arg ref="beanThree"/>
    </bean>

    <bean id="beanTwo" class="x.y.ThingTwo"/>
    <bean id="beanThree" class="x.y.ThingThree"/>
</beans>
```

但如果参数为基本类型，通过默认的参数类型匹配不行，Spring无法判断值的类型，需要通过`type`属性来明确指定构造器参数的类型

```java
package examples;

public class ExampleBean {
    // Number of years to calculate the Ultimate Answer
    private final int years;
    // The Answer to Life, the Universe, and Everything
    private final String ultimateAnswer;

    public ExampleBean(int years, String ultimateAnswer) {
        this.years = years;
        this.ultimateAnswer = ultimateAnswer;
    }
}
```

```xml
<bean id="exampleBean" class="examples.ExampleBean">
    <constructor-arg type="int" value="7500000"/>
    <constructor-arg type="java.lang.String" value="42"/>
</bean>
```

**索引（Index）匹配**：使用`index`属性，你也可以明确指定参数的位置顺序（起始为0），不仅可以解决基本类型的问题，也可以解决同时有多个相同类型参数的问题

```xml
<bean id="exampleBean" class="examples.ExampleBean">
    <constructor-arg index="0" value="7500000"/>
    <constructor-arg index="1" value="42"/>
</bean>
```

**参数名称匹配**：使用`name`属性，简单粗暴的方法，就是明确指定参数的名称，从根本上解决歧义

```xml
<bean id="exampleBean" class="examples.ExampleBean">
    <constructor-arg name="years" value="7500000"/>
    <constructor-arg name="ultimateAnswer" value="42"/>
</bean>
```

> 但是官方文档提到，如果要使用该方法，代码需要以debug模式编译（？？？）Spring才能找到构造方法的参数。如果不想这样做，可以明确使用JDK的注解`@ConstructorProperties`明确指定构造方法参数
>
> ```java
> package examples;
> 
> public class ExampleBean {
>     
>     @ConstructorProperties({"years", "ultimateAnswer"})
>     public ExampleBean(int years, String ultimateAnswer) {
>         this.years = years;
>         this.ultimateAnswer = ultimateAnswer;
>     }
> }
> ```

**工厂方法参数注入**：无论静态还是实例工厂方法，同样都是使用`<constructor-arg/>`标签来指定参数

```xml
<bean id="exampleBean" class="examples.ExampleBean" factory-method="createInstance">
    <constructor-arg ref="anotherExampleBean"/>
    <constructor-arg ref="yetAnotherBean"/>
    <constructor-arg value="1"/>
</bean>

<bean id="anotherExampleBean" class="examples.AnotherBean"/>
<bean id="yetAnotherBean" class="examples.YetAnotherBean"/>
```

#### Setter方法依赖注入

```xml
<bean id="exampleBean" class="examples.ExampleBean">
    <!-- setter injection using the nested ref element -->
    <!-- 可以嵌套ref标签 -->
    <property name="beanOne">
        <ref bean="anotherExampleBean"/>
    </property>

    <!-- setter injection using the neater ref attribute -->
    <property name="beanTwo" ref="yetAnotherBean"/>
    <property name="integerProperty" value="1"/>
</bean>

<bean id="anotherExampleBean" class="examples.AnotherBean"/>
<bean id="yetAnotherBean" class="examples.YetAnotherBean"/>
```

ApplicationContext支持同时使用构造方法注入和Setter方法注入的方式。但是应该使用哪一种，应该遵循：

**使用构造方法注入强制依赖，使用Setter方法注入可选依赖**

所以，在使用构造方法注入依赖时，应该校验参数的合法性

而使用Setter方法注入时，可以在setter方法上使用`@Required`注解，使该依赖作为强制依赖

#### 采用何种方式注入？

Spring团队建议采用构造方法注入的方式，理由如下：

- 采用构造方法注入时，建议在构造方法内部对入参进行校验。如此一来可以保证必须的依赖不为null
- 采用构造方法注入，对象则是不可变的
- 采用构造方法注入，返回的即是初始化完成的对象
- 构造方法的参数过多代表代码不够整洁，需要进行拆分重构

Setter方法注入的使用场景：

- 用于可选（optional）依赖的注入，该依赖可以赋予默认值，否则在使用该依赖的每个地方都需要确保not null
- setter方法注入可以方便后续重新构造和注入

#### 依赖解析过程

> **循环依赖**问题：当使用构造器注入的方式时，可能会产生循环依赖的问题。
>
> 比如A构造器参数依赖B，B构造器参数依赖A，那么在配置A和B时，Spring会检测出该循环依赖，抛出运行时异常`BeanCurrentlyInCreationException`
>
> 解决方法就是，将某些类的注入方式改为setter方式注入，或者完全采用setter方式注入
>
> 在这种情况下，可以强制将某个还未初始化完成的bean注入到另一个bean内

Spring容器在创建的时候，会校验bean配置的合法性，但是属性依赖的注入是在bean创建的时候才会注入。

那么bean什么时候才会创建呢？

Spring容器在加载过程中会检测配置问题，比如引用不存在的bean，或者循环依赖的问题。但是bean的创建和依赖注入则是**懒加载**（initialize lazily）的，这也就意味着，正常启动的Spring容器，使用其中的bean时，在创建bean或者其依赖的过程中，仍可能抛出异常。这就导致一些问题会在使用时才会被发现。

所以`ApplicationContext`**对于单例模式的bean默认进行预实例化**（pre-instantiated），这样在容器启动的过程中就可以发现问题，而不是在真正使用的时候才发现。否则都是懒加载。

### 1.4.2 依赖和配置详解

**直接值（Straight Values）**：指的Java基本类型以及String，在`<property/>`和`<constructor-arg/>`标签的`value`属性中，可以直接赋值，Spring的内置转换服务可以将String类型转为实际类型

还可以使用`<value/>`标签（不同于`value`属性值），Spring可以将`<value/>`标签内的文本转换为`java.util.Properties`对象（使用`PropertyEditor`）

```xml
<bean id="mappings" class="org.springframework.context.support.PropertySourcesPlaceholderConfigurer">
    <!-- typed as a java.util.Properties -->
    <property name="properties">
        <value>
            jdbc.driver.className=com.mysql.jdbc.Driver
            jdbc.url=jdbc:mysql://localhost:3306/mydb
        </value>
    </property>
</bean>
```

**idref标签**：这是一个很奇怪的标签，也是通过其他bean的id来引用该依赖的，感觉就等同于`<ref/>`

```xml
<bean id="theTargetBean" class="..."/>

<bean id="theClientBean" class="...">
    <property name="targetName">
        <idref bean="theTargetBean"/>
    </property>
</bean>
```

等同于下面的代码，注意是在`<property/>`标签内，通过`value`属性来引用其他bean的id？？？

```xml
<bean id="theTargetBean" class="..." />

<bean id="client" class="...">
    <!-- 注意这里是通过value来引用其他bean id的-->
    <property name="targetName" value="theTargetBean"/>
</bean>
```

通过`idref`（或者`ref`）和`value`来引用其他bean id的区别在于：

`idref`（或者`ref`）标签会在部署过程中**校验被引用的bean是否存在**，而`value`属性则不会，单纯把该属性值看做一个字符串

**引用其他bean**：

通过`<ref bean="..."/>`可以引用其他bean（setter方法或者构造器），`bean`属性中可以指定目标bean的`id`属性，或者`name`属性中的任一个值

`<ref parent="..."/>`中 的`parent`可以引用当前容器的**父容器**中的bean（必须是父容器）。当需要对父容器的已知bean做一层封装（封装为同名的bean），则可以使用该方法

```xml
<!-- in the parent context -->
<bean id="accountService" class="com.something.SimpleAccountService">
    <!-- insert dependencies as required here -->
</bean>
```

```xml
<!-- in the child (descendant) context -->
<bean id="accountService" <!-- bean name is the same as the parent bean -->
    class="org.springframework.aop.framework.ProxyFactoryBean">
    <property name="target">
        <ref parent="accountService"/> <!-- notice how we refer to the parent bean -->
    </property>
    <!-- insert other configuration and dependencies as required here -->
</bean>
```

**内部bean**（Inner Bean）：或者称之为嵌套bean更合适，即在该注入的地方直接通过`<bean/>`标签来定义所依赖的bean，不需要指定对其他bean的id引用。

```xml
<bean id="outer" class="...">
    <!-- instead of using a reference to a target bean, simply define the target bean inline -->
    <property name="target">
        <bean class="com.example.Person"> <!-- this is the inner bean -->
            <property name="name" value="Fiona Apple"/>
            <property name="age" value="25"/>
        </bean>
    </property>
</bean>
```

- 内部bean不需要指定id或者name属性，即使指定，Spring也会忽略
- 内部bean的`scope`属性同样会被忽略
- 内部bean始终是一个匿名的bean，只在包含它的外部bean的范围内创建，同时其生命周期范围（scope）与外部bean相同
- 除了外部bean之外，其他bean无法引用内部bean，也就无法将其作为依赖注入

**集合**： `<list/>`, `<set/>`, `<map/>`, and `<props/>` 标签可以分别设置`List`, `Set`, `Map`, and `Properties`类型的依赖

```xml
<bean id="moreComplexObject" class="example.ComplexObject">
    <!-- results in a setAdminEmails(java.util.Properties) call -->
    <property name="adminEmails">
        <props>
            <prop key="administrator">administrator@example.org</prop>
            <prop key="support">support@example.org</prop>
            <prop key="development">development@example.org</prop>
        </props>
    </property>
    <!-- results in a setSomeList(java.util.List) call -->
    <property name="someList">
        <list>
            <value>a list element followed by a reference</value>
            <ref bean="myDataSource" />
        </list>
    </property>
    <!-- results in a setSomeMap(java.util.Map) call -->
    <property name="someMap">
        <map>
            <entry key="an entry" value="just some string"/>
            <entry key="a ref" value-ref="myDataSource"/>
            <entry key="key1">
                <ref bean="..."/>
            </entry>
        </map>
    </property>
    <!-- results in a setSomeSet(java.util.Set) call -->
    <property name="someSet">
        <set>
            <value>just some string</value>
            <ref bean="myDataSource" />
        </set>
    </property>
</bean>
```

**集合合并**：集合的继承与合并（重写）。按我理解，应该是继承某个bean，并对父bean中的 `<list/>`, `<set/>`, `<map/>`, and `<props/>` 依赖进行继承和合并（重写）。

注意`<props/>`标签中的`merge`属性，为true时会让子bean继承和重写父bean的配置。对于`<set/>`和`<map/>`标签来说，同样是“重写（override）”的概念，因为集合类型为无序类型，且键具有唯一性。

而对于`<list/>`标签，集合元素其实是有序的，那么就是”合并“的概念，子bean会同时合并父bean的全部元素，同时，父bean的元素在排在子bean的元素之前。

注意：不同集合类型之间不能合并，否则会报错；同时，`merge`属性只能在更底层的子bean中定义，在父bean中定义无效

```xml
<beans>
    <bean id="parent" abstract="true" class="example.ComplexObject">
        <property name="adminEmails">
            <props>
                <prop key="administrator">administrator@example.com</prop>
                <prop key="support">support@example.com</prop>
            </props>
        </property>
    </bean>
    <!--继承父bean-->
    <bean id="child" parent="parent">
        <property name="adminEmails">
            <!-- the merge is specified on the child collection definition -->
            <props merge="true">
                <!-- 子bean会继承父bean的administrator键值-->
                <prop key="sales">sales@example.com</prop>
                <!--同时会重写父bean的support键值-->
                <prop key="support">support@example.co.uk</prop>
            </props>
        </property>
    </bean>
<beans>
```

**强类型集合**（Strongly-typed collection）：即Java中的泛型，泛型使得Java集合必须存放特定一致类型的元素，在编译时校验，所以称为一种”强类型“的集合。Spring通过内置的类型转换器，通过反射得到泛型的类型参数，从而将String类型转换为实际需要的类型

**Null或者空值**：空字符串值直接使用`""`，null值则使用`<null/>`标签

```xml
<bean class="ExampleBean">
    <property name="email" value=""/>
</bean>

<bean class="ExampleBean">
    <property name="email">
        <null/>
    </property>
</bean>
```

**XML配置快捷方式**：使用p命名空间（p-namespace），可以在`<bean/>`标签的属性中（而不是使用嵌套的`<property/>`标签），直接通过`p:字段名`来指定字段值

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:p="http://www.springframework.org/schema/p"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean name="classic" class="com.example.ExampleBean">
        <property name="email" value="someone@somewhere.com"/>
    </bean>

    <bean name="p-namespace" class="com.example.ExampleBean"
        p:email="someone@somewhere.com"/>
</beans>
```

还可以通过`p:字段名-ref`来指定字段的引用，如下例，通过在标签属性中使用`p:spouse-ref="jane"`来指定`john-modern`这个bean的字段`spouse`引用另一个bean：`jane`

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:p="http://www.springframework.org/schema/p"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean name="john-classic" class="com.example.Person">
        <property name="name" value="John Doe"/>
        <property name="spouse" ref="jane"/>
    </bean>

    <bean name="john-modern" class="com.example.Person"
        p:name="John Doe"
        p:spouse-ref="jane"/>

    <bean name="jane" class="com.example.Person">
        <property name="name" value="Jane Doe"/>
    </bean>
</beans>
```

同理还有c命名空间（c-namespace），即在`<bean/>`标签内的属性中通过`c:构造方法参数名`的格式来配置构造方法参数，而不是使用嵌套的`<constructor-arg/>`

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:c="http://www.springframework.org/schema/c"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="beanTwo" class="x.y.ThingTwo"/>
    <bean id="beanThree" class="x.y.ThingThree"/>

    <!-- traditional declaration with optional argument names -->
    <bean id="beanOne" class="x.y.ThingOne">
        <constructor-arg name="thingTwo" ref="beanTwo"/>
        <constructor-arg name="thingThree" ref="beanThree"/>
        <constructor-arg name="email" value="something@somewhere.com"/>
    </bean>

    <!-- c-namespace declaration with argument names -->
    <bean id="beanOne" class="x.y.ThingOne" c:thingTwo-ref="beanTwo"
        c:thingThree-ref="beanThree" c:email="something@somewhere.com"/>

</beans>
```

使用命名空间的快捷方式其实并不推荐，因为不够简单直观，纯属秀操作行为

**复合字段名称（Compound Property Names）**：也不知道该怎么翻译，直接看示例。`something`有个属性`fred`，`fred`有个属性`bob`，`bob`有个属性`sammy`，可以通过引用链来给`sammy`赋值。不过必须保证在`something`这bean创建之后`fred`和`bob`属性都不为`null`，否则会报NPE

```xml
<bean id="something" class="things.ThingOne">
    <property name="fred.bob.sammy" value="123" />
</bean>
```

### 1.4.3 使用`depends-on`

如何理解`depends-on`的使用场景？

使用`<ref/>`标签通常是为了明确指定某个bean的字段依赖另一个bean，但在某些场景下，两个bean之间并没有这么直接且明确的依赖关系，比如bean-1依赖bean-2先实例并初始化，但是bean-2并不是作为bean-1的属性

```xml
<bean id="beanOne" class="ExampleBean" depends-on="manager"/>
<bean id="manager" class="ManagerBean" />
```

这个时候，在初始化`beanOne`之前会先强制初始化`manager`

如果前置依赖多个bean，可以使用逗号、空格、分号来分割多个bean

```xml
<bean id="beanOne" class="ExampleBean" depends-on="manager,accountDao">
    <property name="manager" ref="manager" />
</bean>

<bean id="manager" class="ManagerBean" />
<bean id="accountDao" class="x.y.jdbc.JdbcAccountDao" />
```

在上面这个例子中，`manager`也可以作为`beanOne`的一个属性

另外，`depends-on`不仅确定”初始化阶段“的依赖，同时确定”销毁阶段“的依赖。比如上例中，`beanOne`会比`manager`先销毁（即定义`depends-on`关系的bean会先被销毁）

### 1.4.4 bean懒加载

默认情况下，`ApplicationContext`容器在初始化阶段就会创建并配置单例beans，这种预实例化是为了能立刻发现配置或者环境因素导致的异常。

但你也可以在bean定义明确关闭预实例化，使用`lazy-init`属性

```xml
<bean id="lazy" class="com.something.ExpensiveToCreateBean" lazy-init="true"/>
<bean name="not.lazy" class="com.something.AnotherBean"/>
```

但是，如果懒加载的bean是非懒加载bean的依赖，那么懒加载是无效的

可以在容器层面上默认使用懒加载，使用`<beans/>`标签的`default-lazy-init`属性

```xml
<beans default-lazy-init="true">
    <!-- no beans will be pre-instantiated... -->
</beans>
```

### 1.4.5 自动装配（Autowiring Collaborators）

Spring可以使用自动装配，而不用明确指定字段或构造方法的依赖。使用`<bean/>`标签的`autowire`属性可以开启自动装配模式

| 模式        | 解释                                                         |
| ----------- | ------------------------------------------------------------ |
| no          | 默认是这种模式，bean引用必须明确使用`<ref/>`标签。更推荐使用该模式，更加清楚明了，明确描述整个系统的依赖 |
| byname      | 按照字段**名称**自动装配，匹配字段名和bean的name             |
| bytype      | 按照字段**类型**自动装配，匹配字段类型和bean的类型。如果有同类型有多个bean则会抛出异常！如果没有该类型的bean，则不会抛出异常，字段值未设置 |
| constructor | 类似`byType`，但是用于构造方法参数。同样的，如果同类型有多个bean则会抛出异常！ |

使用`byType`和`constructor`可以自动装配数组或者集合类型。所有目标类型的bean都会被注入到集合中。并且，如果`Map`的key是String类型，那么也可以自动注入，key即为bean的name，value即为目标类型的bean

**自动装配的限制/缺点**：

- 如果明确指定了依赖，则`autowire`模式无效。
- 不能自动装配基本类型、String、Class以及他们的数组集合
- 如果符合类型的bean不止一个，且字段并不是数组、集合或`Map`类型，且有没有自己明确指定依赖，则会抛出异常。此时只能：
  - 要么关闭自动装配
  - 或者将`<bean/>`标签的 `autowire-candidate` 属性置为`false`，可以把这个bean排除在自动装配的候选之外
  - 或者将`<bean/>`标签的 `primary` 属性置为`true`，可以把该bean作为第一候选
  - 或者实现更细粒度的接口

**自动装配时排除某个bean**：

如上所述，可以将`<bean/>`标签的 `autowire-candidate` 属性置为`false`，可以把这个bean排除在自动装配的候选之外。但只针对`byType`类型的自动装配，对于明确通过name指定依赖的则不起作用

同时，可以使用通配符来限制自动装配的候选bean范围：在顶层的`<beans/>`的 `default-autowire-candidates` 属性中，可以定义多个模式，如 `*Repository`表示name以 `Repository`结尾的bean，多个模式以逗号分隔。但是要注意，如果`<bean/>`明确注明了 `autowire-candidate` 属性（为true or false），则模式匹配不起作用。即 `autowire-candidate` 属性规则最优先。

需要明确 `autowire-candidate` 属性的概念，定义了 `autowire-candidate` 属性的bean只是代表该bean不会作为其他bean自动装配时的依赖，该bean本身还是可以通过自动装配来初始化的。

### 1.4.6 方法注入（Method Injection）

在大多数场景中，容器中的bean都是单例的。但是，当bean的生命周期不同时，会有一个问题：假如单例bean-A（singleton）依赖非单例bean-B（non-singleton，即prototype，通常是有状态的bean），容器只会创建并初始化A一次，即只能为A设置一次依赖，每次使用A的时候没法为其赋值新的B实例。

一种方法是违背控制反转原则，让A实现`ApplicationContextAware`接口，每次使用B前都调用`getBean("B")`来获取最新的bean。但业务代码需要感知并与Spring框架耦合。

这种情况下可以使用Method Injection。

**查找方法注入（Lookup Method Injection）**

在上述问题中，A对B的依赖，不能再通过字段来引用，而是每次使用B之前，都调用一个类似`createB()`这样的方法来获取最新的B实例。那么如何==在不依赖Spring API的前提下==`createB()`调用时每次都返回最新的B实例？这就需要使用到**Lookup Method Injection**。

*Lookup Method Injection*可以重写bean中类似`createB()`这样的方法，并返回容器中prototype类型的bean（*使用CGLIB的字节码生成，来创建bean-A的子类来重写`createB()`方法*）

- 为了能生成子类，bean-A及其`createB()`方法不能是final
- Lookup Method Injection**不适用于**工厂方法或者使用`@Bean`注解的方法创建的bean，因为其创建过程在方法内部，不由容器控制，所以无法在运行时动态生成子类

```java
package fiona.apple;

// no more Spring imports!

public abstract class CommandManager {

    public Object process(Object commandState) {
        // grab a new instance of the appropriate Command interface
        Command command = createCommand();
        // set the state on the (hopefully brand new) Command instance
        command.setState(commandState);
        return command.execute();
    }

    // okay... but where is the implementation of this method?
    protected abstract Command createCommand();
}
```

在这个例子中，`CommandManager`的`createCommand()`方法为抽象方法，依赖容器注入。

这个方法需要满足如下签名格式：`<public|protected> [abstract] <return-type> theMethodName(no-arguments);`

可以是抽象方法，也可以是实例方法，方法无入参。动态生成的子类会重写该方法。

```xml
<!-- a stateful bean deployed as a prototype (non-singleton) -->
<bean id="myCommand" class="fiona.apple.AsyncCommand" scope="prototype">
    <!-- inject dependencies here as required -->
</bean>

<!-- commandProcessor uses statefulCommandHelper -->
<bean id="commandManager" class="fiona.apple.CommandManager">
    <lookup-method name="createCommand" bean="myCommand"/>
</bean>
```

`CommandManager`每次需要`myCommand`的时候都会调用`createCommand()`方法来获取新的实例。如果没有设置`myCommand`为`prototype`，也即为单例，则每次返回的都是同一个`myCommand`实例

如果是基于注解的话，可以使用`@Lookup`

```java
@Component
public abstract class CommandManager {

    public Object process(Object commandState) {
        Command command = createCommand();
        command.setState(commandState);
        return command.execute();
    }

    @Lookup("myCommand")
    protected abstract Command createCommand();
  
    // 或者直接依赖Lookup方法的返回类型自动匹配
    @Lookup
    protected abstract Command createCommand();
}
```

需要注意的是，Spring扫描时会忽略抽象类。而`CommandManager`正是一个抽象类。

在上述xml配置中，抽象类`CommandManager`被明确注册到配置文件中，并且通过`<lookup-method/>`标签，可以通过动态生成子类的方式来生成bean。

而在基于注解的配置中，除了`@Lookup`注解之外，也需要为抽象类`CommandManager`标注`@Component`，以将其明确注册到容器中。

所以，明确注册或者导入到容器中的bean抽象类，则不会被忽略。

**任意方法注入（Arbitrary Method Replacement）**

这个了解即可，感觉略麻烦，不如直接使用查找方法注入。

任意方法注入允许你，使用其他方法实现来替换已有的目标方法。

首先，`MyValueCalculator.computeValue`是我们想要替换的方法

```java
public class MyValueCalculator {

    public String computeValue(String input) {
        // some real code...
    }

    // some other methods...
}
```

`ReplacementComputeValue`会提供新的方法实现，但是需要实现Spring接口`org.springframework.beans.factory.support.MethodReplacer` 

```java
/**
 * meant to be used to override the existing computeValue(String)
 * implementation in MyValueCalculator
 */
public class ReplacementComputeValue implements MethodReplacer {

    public Object reimplement(Object o, Method m, Object[] args) throws Throwable {
        // get the input value, work with it, and return a computed result
        String input = (String) args[0];
        ...
        return ...;
    }
}
```

我们在声明`MyValueCalculator`的bean定义时，需要配置方法替换。

可以使用`<arg-type/>`标签来指定需要被替换的方法的签名，通常在方法入参比较复杂，有多种变量类型的时候才会使用。

类型字符串可以使用缩写，比如：`java.lang.String`或`String`或`Str`

```xml
<bean id="myValueCalculator" class="x.y.z.MyValueCalculator">
    <!-- arbitrary method replacement -->
    <replaced-method name="computeValue" replacer="replacementComputeValue">
        <arg-type>String</arg-type>
    </replaced-method>
</bean>

<bean id="replacementComputeValue" class="a.b.c.ReplacementComputeValue"/>
```

## 1.5 Bean Scopes

| scope       | desc                                                         |
| ----------- | ------------------------------------------------------------ |
| singleton   | 单例模式（默认）                                             |
| prototype   | 原型模式，每次调用都生成新的bean实例                         |
| request     | HTTP Request的生命周期范围内，每次请求都会创建该次请求独有的bean实例。必须使用`ApplicationContext` |
| session     | HTTP Session的生命周期范围内。必须使用web-aware的`ApplicationContext`实现 |
| application | ServletContext的生命周期范围内。必须使用web-aware的`ApplicationContext`实现 |
| websocket   | WebSocket的生命周期范围内。必须使用web-aware的`ApplicationContext`实现 |

### 1.5.1 单例Scope

```xml
<bean id="accountService" class="com.something.DefaultAccountService"/>

<!-- the following is equivalent, though redundant (singleton scope is the default) -->
<bean id="accountService" class="com.something.DefaultAccountService" scope="singleton"/>
```

### 1.5.2 原型Scope

每次请求bean时都会创建bean的新的实例。这里的请求指的是：将其注入某个bean中时（注入之后就不会变了），或者显式地通过 `getBean()` 获取时

有状态的bean应当设置为`prototype`，而无状态的bean则应设置为`singleton`

```xml
<bean id="accountService" class="com.something.DefaultAccountService" scope="prototype"/>
```

与其他的scope相反，Spring不会管理原型bean的整个生命周期范围，因为容器初始化prototype bean之后就将其交给客户端，并不会保留prototype bean的记录。Spring会调用所有scope的bean的**初始化**的生命周期回调（`<bean/>`标签中的`init-method`属性），但是对于prototype，销毁的生命周期回调（`<bean/>`标签中的`destroy-method`属性）则不会被调用

所以这里必须要注意的是：客户端在使用prototype scope的bean之后需要自己销毁，并释放资源（数据库连接等等）

如果需要Spring来协助释放资源，可以使用`bean post-processor`，保有对prototype bean的引用以便后续清理

从以上来看，对于prototype bean来说，Spring容器只相当于Java中的`new`命名，只创建一个新的bean，后续的管理则完全交由客户端

### 1.5.3 单例bean依赖原型bean

单例bean的依赖解析和注入只在bean的实例化阶段，依赖只会被注入一次，也即，如果单例bean依赖prototype bean，那么单例bean只会被注入一次新的prototype bean。如果需要在运行时每次都获取到新的prototype bean，那么需要使用[方法注入](#1.4.6 方法注入（Method Injection）)

### 1.5.4 其他scope

其他四个scope（web-scoped）必须在web-aware的`ApplicationContext`实现中使用，比如 `XmlWebApplicationContext`。如果在其他比如 `ClassPathXmlApplicationContext`的容器中使用其他四种scope，会抛出 `IllegalStateException` 

#### 使用AOP代理进行依赖注入

假设想要将生命周期较短的bean注入到生命周期较长的bean中，比如request-scope的bean注入到singleton bean中，可以使用AOP proxy的方式，即为singleton bean注入代理对象，由代理对象来查找具体的目标对象并进行方法调用

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:aop="http://www.springframework.org/schema/aop"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/aop
        https://www.springframework.org/schema/aop/spring-aop.xsd">

    <!-- an HTTP Session-scoped bean exposed as a proxy -->
    <bean id="userPreferences" class="com.something.UserPreferences" scope="session">
        <!-- instructs the container to proxy the surrounding bean -->
        <!--该标签表明userPreferences需要进行代理-->
        <aop:scoped-proxy/> 
    </bean>

    <!-- a singleton-scoped bean injected with a proxy to the above bean -->
    <bean id="userService" class="com.something.SimpleUserService">
        <!-- a reference to the proxied userPreferences bean -->
        <property name="userPreferences" ref="userPreferences"/>
    </bean>
</beans>
```

为什么需要在这里使用代理？假如不使用代理，那么xml配置如下。同样的问题，`userPreferences`是一个短生命周期bean，而单例的`userManager`是一个长生命周期的bean，但是通过字段注入的方式，`userPreferences`只会被注入一次，即使用的`userPreferences`始终是同一个。

```xml
<bean id="userPreferences" class="com.something.UserPreferences" scope="session"/>

<bean id="userService" class="com.something.SimpleUserService">
    <property name="userPreferences" ref="userPreferences"/>
</bean>
```

因此需要`<aop:scoped-proxy/>`来对短生命周期的bean进行代理。容器实际上为`userService`注入了一个代理对象，当调用`userPreferences`方法时，实际调用的是代理对象的方法，代理对象会从HTTP Session中获取实际的`userPreferences`对象。

> 注意：当在`FactoryBean`的bean定义内使用`<aop:scoped-proxy/>`，则被代理的并不是`FactoryBean.getObject()`方法返回的对象，而是`FactoryBean`本身

**代理方式**

Spring为标记了`<aop:scoped-proxy/>`的bean创建代理对象时，使用CGLIB，也即创建子类并重写方法的方式。CGLIB方式只能拦截代理`public`方法。

也可以使用JDK原生的基于接口的代理方法，这样无需引入CGLIB，但是被代理的bean就必须至少实现一个接口

```xml
<!-- DefaultUserPreferences implements the UserPreferences interface -->
<bean id="userPreferences" class="com.stuff.DefaultUserPreferences" scope="session">
    <aop:scoped-proxy proxy-target-class="false"/>
</bean>

<bean id="userService" class="com.something.SimpleUserService">
    <property name="userPreferences" ref="userPreferences"/>
</bean>
```

### 1.5.5 自定义scope

**（1）创建自定义scope**

实现Spring API：`org.springframework.beans.factory.config.Scope`

```java
Object get(String name, ObjectFactory<?> objectFactory);
Object remove(String name);
void registerDestructionCallback(String name, Runnable destructionCallback);
String getConversationId();
```

**（2）使用自定义scope**

使用 `ConfigurableBeanFactory` 定义的方法来注册scope

```java
void registerScope(String scopeName, Scope scope);
```

注册之后，可以在bean定义通过`scopeName`来指定bean为自定义的scope

```java
Scope threadScope = new SimpleThreadScope();
beanFactory.registerScope("thread", threadScope);
```

```xml
<bean id="..." class="..." scope="thread">
```

除了以上编码的方式，还可以通过声明式注册的方法，使用： `CustomScopeConfigurer` 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:aop="http://www.springframework.org/schema/aop"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/aop
        https://www.springframework.org/schema/aop/spring-aop.xsd">

    <bean class="org.springframework.beans.factory.config.CustomScopeConfigurer">
        <property name="scopes">
            <map>
                <entry key="thread">
                    <bean class="org.springframework.context.support.SimpleThreadScope"/>
                </entry>
            </map>
        </property>
    </bean>

    <bean id="thing2" class="x.y.Thing2" scope="thread">
        <property name="name" value="Rick"/>
        <aop:scoped-proxy/>
    </bean>

    <bean id="thing1" class="x.y.Thing1">
        <property name="thing2" ref="thing2"/>
    </bean>
</beans>
```

## 1.6 自定义Bean的特性

### 1.6.1 生命周期回调（Lifecycle Callbacks）

`BeanPostProcessor`：`ApplicationContext`会自动检测实现该接口的bean，并应用于容器中所有创建的bean

 `InitializingBean` ：bean所有依赖都注入完成之后调用，可用于bean自定义初始化，或者校验所有强制依赖是否都注入成功

####  初始化阶段回调（Initialization Callbacks）

 `org.springframework.beans.factory.InitializingBean` 

```java
void afterPropertiesSet() throws Exception;
```

使用该接口的话就需要与Spring API耦合，所以更推荐使用：

- `@PostConstruct`注解
- 或者在xml配置文件中使用`init-method`属性来指定bean中自定义的方法来作为回调方法（必须无参且无返回值）
- 或者使用`@Bean`注解中的`initMethod`属性（在`initMethod`中指定`@Bean`注解标注的方法返回的bean中的初始化回调方法）

#### 销毁阶段回调（Destruction Callbacks）

 `org.springframework.beans.factory.DisposableBean` 

```java
void destroy() throws Exception;
```

当容器要销毁时，会调用` org.springframework.beans.factory.DisposableBean `的预销毁方法。可替代的同样有：

-  [`@PreDestroy`](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-postconstruct-and-predestroy-annotations) 注解
-  `<bean/>`标签中的 `destroy-method` 属性
- `@Bean`注解中的`destroyMethod`属性

#### 默认的初始化和销毁回调

也可以在父标签`<beans/>`中使用`default-init-method/default-destroy-method`属性，指定一批bean的默认初始化/销毁回调方法。

不过，在子标签`<bean/>`中指定`init-method/destroy-method`可以覆盖父标签中的配置

Spring是在设置完依赖之后立刻调用初始化回调，此时bean拥有的是刚刚初始化好的依赖，此时AOP代理还没有应用到该目标bean

#### 结合多种回调配置方式

从上面两小节可以知道，有三种配置初始化/销毁回调的方式：

-  `InitializingBean` 和 `DisposableBean`接口方法
- 自定义的 `init()` 和 `destroy()` 方法
-  `@PostConstruct` 和 `@PreDestroy`注解

如果在一个bean上同时使用了以上三种方式，那么按照如下顺序调用：

1. `@PostConstruct` 和 `@PreDestroy`注解
2.  `InitializingBean` 和 `DisposableBean`接口方法
3. 自定义的 `init()` 和 `destroy()` 方法

看下源码就会知道，`@PostConstruct` 和 `@PreDestroy`注解的调用是通过`InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization`来实现的，这个方法是在bean所有初始化回调（包括 `InitializingBean` 和自定义初始化方法）之前调用的。而`BeanPostProcessor.postProcessAfterInitialization`则是在所有初始化回调方法之后调用的。

#### 容器启动和关闭回调

当`ApplicationContext`启动或关闭时，会调用所有`Lifecycle`接口的实现

```java
public interface Lifecycle {
    void start();
    void stop();
    boolean isRunning();
}
```

实际执行是交给代理来处理的`LifecycleProcessor`，处理器本身也是`Lifecycle`的一个实现，扩展了两个方法，在容器refresh/close时执行

```java
public interface LifecycleProcessor extends Lifecycle {
    void onRefresh();
    void onClose();
}
```

这里的实现挺有借鉴意义的，容器在strart/refresh/stop/close时会去查找`LifecycleProcessor`（没有则会初始化一个），随后调用`LifecycleProcessor`

- 在容器start/refresh/stop/close时需要调用所有`Lifecycle`接口的实现
- Spring通过`LifecycleProcessor`代理，在`LifecycleProcessor.start/stop/onRefresh/onClose`方法内统一去查找并调用所有`Lifecycle`接口的实现
- 而代理本身也是`Lifecycle`接口的实现，在查找时会排除掉自己



再来讨论下启动和关闭过程中的调用顺序。如果定义了`depends-on`依赖关系，假使A依赖B，那么A在B之后启动，并且在B之前销毁。但是有些情况下，并没有这种显示的依赖关系，只能知道某些类型必须在其他类型之前启动。在这种情况下，可以使用`SmartLifecycle`

```java
public interface SmartLifecycle extends Lifecycle, Phased {
    boolean isAutoStartup();
    void stop(Runnable callback);
}
```

`SmartLifecycle`继承自`Lifecycle`和`Phased`

```java
public interface Phased {
    int getPhase();
}
```

容器启动过程中，当对象实现`SmartLifecycle`时，`getPhase()`返回的值越小则越先开始启动，关闭时则越晚销毁。

对于只实现了`Lifecycle`的对象而言，它的`getPhase()`则默认是0。

再看`stop(Runnable callback)`方法，callback里执行的方法原本是`LifecycleProcessor`在执行完`Lifecycle.stop()`方法之后执行的内容，现在交由`SmartLifecycle.stop(Runnable callback)`方法在其内部去异步回调执行。`stop(Runnable callback)`一般在内部调用自身的`stop()`方法之后就必须调用`callback.run()`方法。

最后再看`isAutoStartup()`方法。`LifecycleProcessor`内定义了两个方法，一个`onClose`方法，在容器`close`时被调用，而另一个方法`onRefresh`方法则是在容器刷新时调用，这个时候`LifecycleProcessor`会检查`SmartLifecycle.isAutoStartup()`是否返回true，为true时则会调用该`SmartLifecycle`的`start`方法。































