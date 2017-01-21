package com.ulfric.commons.cdi;

import java.lang.annotation.Annotation;
import java.util.Objects;

import com.ulfric.commons.cdi.scope.Default;
import com.ulfric.commons.cdi.scope.DefaultScopeStrategy;
import com.ulfric.commons.cdi.scope.Scoped;
import com.ulfric.commons.cdi.scope.Shared;
import com.ulfric.commons.cdi.scope.SharedScopeStrategy;

public class ObjectFactory extends Child<ObjectFactory> {

	public static ObjectFactory newInstance()
	{
		return new ObjectFactory();
	}

	private final Bindings bindings;
	private final Scopes scopes;
	private final ImplementationFactory implementationFactory = new ImplementationFactory();
	private final Injector injector = new Injector(this);

	private ObjectFactory()
	{
		this.bindings = new Bindings();
		this.scopes = new Scopes();

		this.init();
	}

	private ObjectFactory(ObjectFactory parent)
	{
		super(parent);

		this.bindings = new Bindings(parent.bindings);
		this.scopes = new Scopes(parent.scopes);

		this.init();
	}

	private void init()
	{
		this.scopes.registerBinding(Default.class, DefaultScopeStrategy.class);
		this.scopes.registerBinding(Shared.class, SharedScopeStrategy.class);
	}

	public Binding bind(Class<?> request)
	{
		Objects.requireNonNull(request);

		return this.bindings.createBinding(request);
	}

	public Binding bindScope(Class<? extends Annotation> request)
	{
		Objects.requireNonNull(request);

		return this.scopes.createBinding(request);
	}

	public Object request(Class<?> request)
	{
		Objects.requireNonNull(request);

		Class<?> implementation = this.bindings.getRegisteredBinding(request);

		if (implementation == null)
		{
			implementation = this.tryToCreateAndRegisterImplementation(request);

			if (implementation == null)
			{
				return null;
			}
		}

		return this.getInjectedObject(implementation);
	}

	private Object getInjectedObject(Class<?> implementation)
	{
		Scoped<?> scoped = this.scopes.getScopedObject(implementation);
		this.injector.injectFields(scoped);
		return scoped.read();
	}

	private Class<?> tryToCreateAndRegisterImplementation(Class<?> request)
	{
		Class<?> implementation = this.implementationFactory.createImplementationClass(request);

		if (implementation != null)
		{
			this.bindings.registerBinding(request, implementation);
		}

		return implementation;
	}

}