import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class Unit {

    private static final int TEST_MAX_NUMBER = 100;
    public static Map<String, Throwable> testClass(String name) {
        Map<String, Throwable> returnMap = new HashMap<>();

        try {
            Class<?> namedClass = Class.forName(name);

            Set<String> seenMethods = new HashSet<>();

            List<Method> beforeClassMethods = getStaticMethods(namedClass, BeforeClass.class, seenMethods);
            List<Method> afterClassMethods = getStaticMethods(namedClass, AfterClass.class, seenMethods);
            List<Method> beforeMethods = getInstanceMethods(namedClass, Before.class, seenMethods);
            List<Method> afterMethods = getInstanceMethods(namedClass, After.class, seenMethods);
            List<Method> testingMethods = getInstanceMethods(namedClass, Test.class, seenMethods);

            //Execute beforeClassMethods:
            for (Method method : beforeClassMethods) {
                method.invoke(null);
            }

            for (Method testingMethod : testingMethods) {
                Object newInstance = namedClass.getConstructor().newInstance();

                //Execute beforeMethods:
                for (Method method : beforeMethods) {
                    method.invoke(newInstance);
                }

                try {
                    testingMethod.invoke(newInstance);
                    returnMap.put(testingMethod.getName(), null);
                } catch (InvocationTargetException e) {
                    returnMap.put(testingMethod.getName(), e.getTargetException());
                } catch (Throwable t) {
                    returnMap.put(testingMethod.getName(), t);
                }

                //Execute afterMethods:
                for (Method method : afterMethods) {
                    method.invoke(newInstance);
                }
            }

            //Execute afterClassMethods:
            for (Method method : afterClassMethods) {
                method.invoke(null);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed run tests for testingClass", e);
        }

        return returnMap;
    }


    private static List<Method> getStaticMethods(Class<?> namedClass, Class<? extends Annotation> annotationClass,
                                                 Set<String> seenMethods){

        List<Method> staticMethods = new ArrayList<>();

        for(Method method : namedClass.getDeclaredMethods()) {
            if(method.isAnnotationPresent(annotationClass)) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if(!seenMethods.contains(method.getName())){
                        seenMethods.add(method.getName());
                    } else {
                        throw new RuntimeException("More than one annotation detected: " + method.getName());
                    }
                    staticMethods.add(method);
                } else {
                    throw new NoSuchElementException(method.getName() + " method is not static(StaticMethods)");
                }
            }
        }
        staticMethods.sort(Comparator.comparing(Method::getName));
        return staticMethods;
    }

    private static List<Method> getInstanceMethods(Class<?> namedClass, Class<? extends Annotation> annotationClass,
                                                   Set<String> seenMethods){

        List<Method> instanceMethods = new ArrayList<>();

        for(Method method : namedClass.getDeclaredMethods()) {
            if(method.isAnnotationPresent(annotationClass)) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    if(!seenMethods.contains(method.getName())){
                        seenMethods.add(method.getName());
                    } else {
                        throw new RuntimeException("More than one annotation detected: " + method.getName());
                    }
                    instanceMethods.add(method);
                } else {
                    throw new NoSuchElementException(method.getName() + " method is static(InstanceMethods)");
                }
            }
        }
        instanceMethods.sort(Comparator.comparing(Method::getName));
        return instanceMethods;
    }

    public static Map<String, Object[]> quickCheckClass(String name) {
        Map<String, Object[]> returnFailMap = new HashMap<>();

        try {
            Class<?> namedClass = Class.forName(name);
            Object namedClassInstance = namedClass.getConstructor().newInstance();

            Method[] namedClassMethods = namedClass.getMethods();
            Arrays.sort(namedClassMethods, Comparator.comparing(Method::getName));

            for(Method method : namedClassMethods){
                if(method.isAnnotationPresent(Property.class)){
                    List<List<Object>> argumentCombinations = allParameterCombinations(method, namedClassInstance, namedClass);
                    for (List<Object> arguments : argumentCombinations.subList(0,Math.min(argumentCombinations.size(),TEST_MAX_NUMBER))) {
                        try {
                            boolean result = (Boolean) method.invoke(namedClassInstance, arguments.toArray());
                            if (!result) {
                                returnFailMap.put(method.getName(), arguments.toArray());
                                break;
                            }
                        } catch (Throwable e) {
                            returnFailMap.put(method.getName(), arguments.toArray());
                            break;
                        }
                    }
                    if(!returnFailMap.containsKey(method.getName())){
                        returnFailMap.put(method.getName(), null);
                    }
                    if(returnFailMap.size() > TEST_MAX_NUMBER){
                        throw new IndexOutOfBoundsException("Test Max number exceeded");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return returnFailMap;
    }

    /**
     * Gets the possible inputs and combines them in order to create all the possible input combinations for the method.
     * @param method: The method to find all possible parameters.
     * @return All combinations of parameters as a list of method.
     */
    private static List<List<Object>> allParameterCombinations(Method method, Object instance, Class<?> generatorClass){
        List<List<Object>> resultParameters = new ArrayList<>();

        Parameter[] methodParameters = method.getParameters();
        for(Parameter methodParameter : methodParameters){
            // Find all possible values for given parameter
           List<Object> allPossibleValuesForMethodParameter = allPossibleValues(methodParameter, instance, generatorClass);
           resultParameters = magicMultiply(allPossibleValuesForMethodParameter, resultParameters);
        }
        return resultParameters;
    }

    private static List<Object> allPossibleValues(Parameter parameter, Object instance, Class<?> generatorClass) {
        if(parameter.isAnnotationPresent(IntRange.class)){
            return integerRange(parameter.getAnnotation(IntRange.class));
        } else if(parameter.isAnnotationPresent(StringSet.class)){
            return Arrays.asList(parameter.getAnnotation(StringSet.class).strings());
        } else if(parameter.isAnnotationPresent(ListLength.class)){
            return listLengthRange(parameter.getAnnotation(ListLength.class), parameter.getAnnotatedType(), instance, generatorClass);
        } else if (parameter.isAnnotationPresent(ForAll.class)) {
            return forAllRange(parameter.getAnnotation(ForAll.class), instance, generatorClass);
        } else {
            throw new NoSuchElementException("No annotations found");
        }
    }

    private static List<Object> allPossibleValuesOfAnnotatedType(AnnotatedType annotatedType,
                                                                 Object instance, Class<?> generatorClass) {
        if (!(annotatedType instanceof AnnotatedParameterizedType)) {
            throw new RuntimeException("Not a parametrized type or not annotated");
        }

        AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
        AnnotatedType[] paramActualTypes = paramType.getAnnotatedActualTypeArguments();

        if (paramActualTypes.length != 1) {
            throw new RuntimeException("Parametrized type not supported or not annotated properly");
        }

        ListLength listLengthAnnotation = paramActualTypes[0].getAnnotation(ListLength.class);
        if (listLengthAnnotation != null) {
            return listLengthRange(listLengthAnnotation, paramActualTypes[0], instance, generatorClass);
        }

        IntRange intRangeAnnotation = paramActualTypes[0].getAnnotation(IntRange.class);
        if (intRangeAnnotation != null) {
            return integerRange(intRangeAnnotation);
        }

        StringSet stringSetAnnotation = paramActualTypes[0].getAnnotation(StringSet.class);
        if (stringSetAnnotation != null) {
            return Arrays.asList(stringSetAnnotation.strings());
        }

        ForAll forAllAnnotation = paramActualTypes[0].getAnnotation(ForAll.class);
        if (forAllAnnotation != null) {
            return forAllRange(forAllAnnotation, instance, generatorClass);
        }

        throw new NoSuchElementException("No annotations found on the type");
    }

    private static List<Object> integerRange(IntRange range){
        List<Object> rangeIntegers = new ArrayList<>();
        for(int i = range.min(); i <= range.max(); i++){
            rangeIntegers.add(i);
        }
        return rangeIntegers;
    }

    private static List<Object> listLengthRange(ListLength length, AnnotatedType annotatedType,
                                                Object instance, Class<?> generatorClass){
        List<Object> allPossibleValues = allPossibleValuesOfAnnotatedType(annotatedType, instance, generatorClass);
        List<Object> result = new ArrayList<>();
        for (int i=length.min(); i<= length.max(); i++) {
            List<List<Object>> combinations = allCombinationsOfListWithLength(allPossibleValues, i);
            if (combinations.isEmpty()) {
                result.add(new ArrayList<>());
            } else {
                result.addAll(combinations);
            }
        }
        return result;
    }

    private static List<List<Object>> allCombinationsOfListWithLength(List<Object> elements, int length) {
        if (length == 0) {
            return new ArrayList<>();
        }

        List<List<Object>> result = new ArrayList<>();
        for (int i =0; i<length; i++) {
            result = magicMultiply(elements, result);
        }

        return result;
    }

    private static List<List<Object>> magicMultiply(List<Object> parameterValues, List<List<Object>> results){
        List<List<Object>> returnList = new ArrayList<>();

        if(results.isEmpty()){
            for(Object value : parameterValues){
                returnList.add(Collections.singletonList(value));
            }
            return returnList;
        }

        for(List<Object> element : results){
            for (Object addElement : parameterValues){
                List<Object> newList = new ArrayList<>(element);
                newList.add(addElement);
                returnList.add(newList);
            }
        }
        return returnList;
    }

    private static List<Object> forAllRange(ForAll forAll, Object instance, Class<?> generatorClass){
        List<Object> values = new ArrayList<>();
        try {
            Method generatorMethod = generatorClass.getMethod(forAll.name());
            for (int i = 0; i < forAll.times(); i++) {
                values.add(generatorMethod.invoke(instance));
            }
        } catch (Exception e) {
            throw new RuntimeException("Class " + generatorClass.getSimpleName() + " not have " + forAll.name() +
                    " method setup properly.");
        }

        return values;
    }
}
