package com.camptocamp.opendata.producer.geotools;

import java.util.regex.Pattern;

import org.geotools.api.feature.IllegalAttributeException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.factory.Hints;

/**
 * GeoTools {@link PropertyAccessorFactory} that allows to evaluate
 * {@link SimpleFeatureType} and {@link SimpleFeature} attributes containing any
 * unicode character, spaces, and punctuation signs in their names, as long as
 * they start with a (unicode) letter.
 */
public class UnicodeSimpleFeaturePropertyAccessorFactory implements PropertyAccessorFactory {

    public static final PropertyAccessor UNRESTRICTED_ATTRIBUTE_NAME_ACCESS = new UnrestrictedUnicodePropertyAccessor();

    /**
     * https://regex101.com/r/XCpNjr/1
     */
    static final Pattern PATTERN = Pattern.compile("(\\p{L}(\\s*\\p{Punct}*\\p{L}*\\d*)*)");

    @SuppressWarnings("rawtypes")
    public @Override PropertyAccessor createPropertyAccessor(Class type, String xpath, Class target, Hints hints) {
        if (null == xpath || null == type) {
            return null;
        }

        if (SimpleFeature.class.isAssignableFrom(type) || SimpleFeatureType.class.isAssignableFrom(type)) {
            return PATTERN.matcher(xpath).matches() ? UNRESTRICTED_ATTRIBUTE_NAME_ACCESS : null;
        }

        return null;
    }

    static class UnrestrictedUnicodePropertyAccessor implements PropertyAccessor {

        public @Override boolean canHandle(Object object, String xpath, Class<?> target) {

            if (object instanceof SimpleFeatureType) {
                return ((SimpleFeatureType) object).indexOf(xpath) >= 0;
            } else if (object instanceof SimpleFeature) {
                return ((SimpleFeature) object).getType().indexOf(xpath) >= 0;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        public @Override <T> T get(Object object, String xpath, Class<T> target) {

            if (object instanceof SimpleFeature) {
                return (T) ((SimpleFeature) object).getAttribute(xpath);
            } else if (object instanceof SimpleFeatureType) {
                return (T) ((SimpleFeatureType) object).getDescriptor(xpath);
            }

            return null;
        }

        @SuppressWarnings("rawtypes")
        public @Override void set(Object object, String xpath, Object value, Class target)
                throws IllegalAttributeException {

            if (object instanceof SimpleFeature) {
                ((SimpleFeature) object).setAttribute(xpath, value);
            }
            throw new IllegalAttributeException("FeatureType type is immutable");
        }
    }
}
