package com.ulfric.dragoon.constrain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.internal.Primitives;
import com.ulfric.commons.exception.Try;
import com.ulfric.dragoon.construct.InstanceUtils;

public enum Constraints {

	;

	private static final Map<Class<? extends ConstraintAdapter<?>>, ConstraintAdapter<?>> ADAPTERS = new IdentityHashMap<>();

	public static void check(Object object) throws ConstraintException
	{
		for (Field field : object.getClass().getDeclaredFields())
		{
			if (Constraints.isConstrainable(field))
			{
				field.setAccessible(true);
				Constraints.check(object, field);
			}
		}
	}

	private static void check(Object object, Field field) throws ConstraintException
	{
		List<Constraint> constraints = Constraints.getConstrainingAnnotations(field);

		for (Constraint constraint : constraints)
		{
			@SuppressWarnings("unchecked")
			ConstraintAdapter<Object> adapter = (ConstraintAdapter<Object>)
					Constraints.ADAPTERS.computeIfAbsent(constraint.adapter(), Constraints::createAdapter);

			Constraints.ensureFieldMatchesAdapter(field, adapter);

			adapter.check(Try.to(() -> field.get(object)));
		}
	}

	private static void ensureFieldMatchesAdapter(Field field, ConstraintAdapter<?> adapter) throws ConstraintException
	{
		if (!adapter.adaptionType().isAssignableFrom(Primitives.wrap(field.getType())))
		{
			throw new ConstraintTypeMismatchException(
					"Adapter type [" + adapter.adaptionType().getName() +
							"] does not match field type [" + field.getType().getName() + "]"
			);
		}
	}

	private static ConstraintAdapter<?> createAdapter(Class<? extends ConstraintAdapter<?>> clazz)
	{
		return InstanceUtils.createOrNull(clazz);
	}

	private static boolean isConstrainable(Field field)
	{
		int modifers = field.getModifiers();
		boolean annotationPresent = !Constraints.getConstrainingAnnotations(field).isEmpty();

		return !Modifier.isStatic(modifers) && annotationPresent;
	}

	private static List<Constraint> getConstrainingAnnotations(Field field)
	{
		return Stream.of(field.getDeclaredAnnotations())
				.filter(Constraints::isConstraint)
				.map(annotation -> annotation.annotationType().getAnnotation(Constraint.class))
				.collect(Collectors.toList());
	}

	private static boolean isConstraint(Annotation annotation)
	{
		return annotation.annotationType().isAnnotationPresent(Constraint.class);
	}

}
