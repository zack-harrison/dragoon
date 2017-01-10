package com.ulfric.commons.cdi.inject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.ulfric.commons.cdi.construct.BeanFactory;
import com.ulfric.commons.collect.MapUtils;
import com.ulfric.commons.exception.Try;
import com.ulfric.commons.reflect.HandleUtils;

public final class Injector {

	public static Injector newInstance(BeanFactory factory)
	{
		Objects.requireNonNull(factory);

		return new Injector(factory);
	}

	private final Map<Class<?>, InjectionData> profiles;
	final BeanFactory factory;

	private Injector(BeanFactory factory)
	{
		this.factory = factory;
		this.profiles = MapUtils.newSynchronizedIdentityHashMap();
	}

	public void injectState(Object object)
	{
		Objects.requireNonNull(object);

		this.getInjectionData(object).inject(object);
	}

	private InjectionData getInjectionData(Object object)
	{
		return this.profiles.computeIfAbsent(object.getClass(), InjectionData::new);
	}

	private final class InjectionData
	{

		private final Class<?> clazz;
		private final Map<Class<?>, List<MethodHandle>> mutators;

		InjectionData(Class<?> clazz)
		{
			this.clazz = clazz;
			this.mutators = new IdentityHashMap<>();
			this.resolveMutators();
		}

		private void resolveMutators()
		{
			FieldUtils.getAllFieldsList(this.clazz)
				.stream()
				.filter(this::isInjectable)
				.forEach(field ->
				{
					field.setAccessible(true);
					MethodHandle handle = HandleUtils.createSetter(field)
							.asType(MethodType.methodType(void.class, Object.class, Object.class));

					this.mutators.computeIfAbsent(field.getType(), ignore -> new ArrayList<>()).add(handle);
				});
		}

		private boolean isInjectable(Field field)
		{
			if (Modifier.isStatic(field.getModifiers()))
			{
				return false;
			}

			if (field.getType().isPrimitive())
			{
				return false;
			}

			if (!field.isAnnotationPresent(Inject.class))
			{
				return false;
			}

			return true;
		}

		void inject(Object object)
		{
			this.mutators.forEach((type, handles) ->
			{
				handles.forEach(handle ->
				{
					Object created = Injector.this.factory.request(type);
					this.tryToInvokeVoid(handle, object, created);
				});
			});
		}

		private void tryToInvokeVoid(MethodHandle handle, Object object, Object created)
		{
			Try.to(() ->
			{
				handle.invokeExact(object, created);
			});
		}
	}

}