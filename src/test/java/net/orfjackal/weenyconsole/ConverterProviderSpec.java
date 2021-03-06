/*
 * This file is part of WeenyConsole <http://www.orfjackal.net/>
 *
 * Copyright (c) 2007-2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *     * Neither the name of the copyright holder nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.weenyconsole;

import jdave.Block;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.weenyconsole.converters.BooleanConverter;
import net.orfjackal.weenyconsole.converters.DelegatingConverter;
import net.orfjackal.weenyconsole.converters.StringConstructorConverter;
import net.orfjackal.weenyconsole.exceptions.ConversionFailedException;
import net.orfjackal.weenyconsole.exceptions.InvalidSourceValueException;
import net.orfjackal.weenyconsole.exceptions.TargetTypeNotSupportedException;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 3.8.2007
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
@RunWith(JDaveRunner.class)
public class ConverterProviderSpec extends Specification<ConverterProvider> {

    private void addConverterToProvider(final ConverterProvider provider, final Converter converter, final Class<?> targetType) {
        checking(new Expectations() {{
            one (converter).supportedTargetType(); will(returnValue(targetType));
            one (converter).setProvider(provider);
        }});
        provider.addConverter(converter);
    }

    public class ProviderWithNoConverters {

        private ConverterProvider provider;

        public ConverterProvider create() {
            provider = new ConverterProvider();
            return provider;
        }

        public void shouldNotProvideAnyConverters() {
            specify(provider.converterFor(Integer.class), should.equal(null));
        }

        public void shouldProvideAConverterAfterItIsFirstAdded() {
            final Converter converter = mock(Converter.class);
            checking(new Expectations() {{
                one (converter).supportedTargetType(); will(returnValue(Integer.class));
                one (converter).setProvider(provider);
            }});
            provider.addConverter(converter);
            specify(provider.converterFor(Integer.class), should.equal(converter));
        }

        public void shouldGiveToTheConverterAccessToTheProvider() {
            final Converter converter = mock(Converter.class);
            checking(new Expectations() {{
                one (converter).supportedTargetType(); will(returnValue(Integer.class));
                one (converter).setProvider(provider);
            }});
            provider.addConverter(converter);
        }

        public void shouldNotAllowConvertersWhoseTargetTypeIsNull() {
            final Converter converter = mock(Converter.class);
            checking(new Expectations() {{
                one (converter).supportedTargetType(); will(returnValue(null));
            }});
            specify(new Block() {
                public void run() throws Throwable {
                    provider.addConverter(converter);
                }
            }, should.raise(IllegalArgumentException.class));
        }
    }

    public class ProviderWithConverters {

        private ConverterProvider provider;
        private Converter integerConverter;
        private Converter doubleConverter;

        public ConverterProvider create() {
            provider = new ConverterProvider();
            integerConverter = mock(Converter.class, "integerConverter");
            doubleConverter = mock(Converter.class, "doubleConverter");
            addConverterToProvider(provider, integerConverter, Integer.class);
            addConverterToProvider(provider, doubleConverter, Double.class);
            return provider;
        }

        public void shouldProvideAConverterForTheRequestedTargetType() {
            specify(provider.converterFor(Integer.class), should.equal(integerConverter));
            specify(provider.converterFor(Double.class), should.equal(doubleConverter));
        }

        public void afterRemovingAConverterTheProviderShouldNotContainIt() {
            checking(new Expectations() {{
                one (integerConverter).setProvider(null);
            }});
            provider.removeConverterFor(Integer.class);
            specify(provider.converterFor(Integer.class), should.equal(null));
            specify(provider.converterFor(Double.class), should.equal(doubleConverter));
        }

        public void removingAConverterWhichDoesNotExistShouldExitSilently() {
            provider.removeConverterFor(String.class);
            specify(provider.converterFor(Integer.class), should.equal(integerConverter));
            specify(provider.converterFor(Double.class), should.equal(doubleConverter));
        }

        public void shouldConvertNullToNull() throws ConversionFailedException {
            specify(provider.valueOf(null, Object.class), should.equal(null));
        }

        public void shouldNotConvertNullToAPrimitiveType() {
            provider.addConverter(new DelegatingConverter(int.class, Integer.class));
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf(null, int.class);
                }
            }, should.raise(InvalidSourceValueException.class));
        }
    }

    public class ProviderWithManyConvertersInTheSameClassHierarchy {

        private ConverterProvider provider;
        private Converter superConverter;
        private Converter exactConverter;
        private Converter subConverter;

        public ConverterProvider create() {
            provider = new ConverterProvider();
            superConverter = mock(Converter.class, "superConverter");
            exactConverter = mock(Converter.class, "exactConverter");
            subConverter = mock(Converter.class, "subConverter");
            addConverterToProvider(provider, superConverter, Object.class);
            addConverterToProvider(provider, exactConverter, Number.class);
            addConverterToProvider(provider, subConverter, Integer.class);
            return provider;
        }

        /**
         * A dedicated converter for the target type, if present, is always the best
         * candidate for conversions, so it should have the highest priority. This will
         * also make it possible to override the default conversion for a specific type,
         * if the default {@link StringConstructorConverter} would not produce the
         * desired result (such as in the case of {@link BooleanConverter}).
         */
        public void shouldFirstlyUseAConverterForTheTargetType() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(returnValue(1));
            }});
            specify(provider.valueOf("1", Number.class), should.equal(1));
        }

        /**
         * By definition all subclasses of the target type are instances of the target type,
         * so using one of the subclasses' converter should be almost as good as a converter
         * for the exact target type.
         */
        public void shouldSecondlyUseAConverterForASubclassOfTheTargetType() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (subConverter  ).valueOf("1", Number.class); will(returnValue(1));
            }});
            specify(provider.valueOf("1", Number.class), should.equal(1));
        }

        /**
         * A generic converter, such as {@link StringConstructorConverter} <em>might</em>
         * be able to handle also the conversions of its subclasses, so it is a good guess
         * to try using the converter of a superclass of the target type.
         */
        public void shouldThirdlyUseAConverterForASuperclassOfTheTargetType() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (subConverter  ).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (superConverter).valueOf("1", Number.class); will(returnValue(1));
            }});
            specify(provider.valueOf("1", Number.class), should.equal(1));
        }

        /**
         * When no suitable converter could be found, the only option is to report
         * it to the user.
         */
        public void shouldFourthlyFail() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (subConverter  ).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (superConverter).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
            }});
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf("1", Number.class);
                }
            }, should.raise(TargetTypeNotSupportedException.class));
        }

        public void shouldFailOnFirstStageIfTheSourceValueIsReportedAsInvalid() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(throwException(new InvalidSourceValueException("1", Number.class)));
            }});
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf("1", Number.class);
                }
            }, should.raise(InvalidSourceValueException.class));
        }

        public void shouldFailOnSecondStageIfTheSourceValueIsReportedAsInvalid() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (subConverter  ).valueOf("1", Number.class); will(throwException(new InvalidSourceValueException("1", Number.class)));
            }});
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf("1", Number.class);
                }
            }, should.raise(InvalidSourceValueException.class));
        }

        public void shouldFailOnThirdStageIfTheSourceValueIsReportedAsInvalid() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (subConverter  ).valueOf("1", Number.class); will(throwException(new TargetTypeNotSupportedException("1", Number.class)));
                one (superConverter).valueOf("1", Number.class); will(throwException(new InvalidSourceValueException("1", Number.class)));
            }});
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf("1", Number.class);
                }
            }, should.raise(InvalidSourceValueException.class));
        }

        public void shouldSkipFirstStageIfNoConverterWasFound() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).setProvider(null);
                one (subConverter  ).valueOf("1", Number.class); will(returnValue(1));
            }});
            provider.removeConverterFor(Number.class);
            specify(provider.valueOf("1", Number.class), should.equal(1));
        }

        public void shouldSkipSecondStageIfNoConverterWasFound() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).setProvider(null);
                one (subConverter  ).setProvider(null);
                one (superConverter).valueOf("1", Number.class); will(returnValue(1));
            }});
            provider.removeConverterFor(Number.class);
            provider.removeConverterFor(Integer.class);
            specify(provider.valueOf("1", Number.class), should.equal(1));
        }

        public void shouldSkipThirdStageAndFailIfNoConverterWasFound() {
            checking(new Expectations() {{
                one (exactConverter).setProvider(null);
                one (subConverter  ).setProvider(null);
                one (superConverter).setProvider(null);
            }});
            provider.removeConverterFor(Number.class);
            provider.removeConverterFor(Integer.class);
            provider.removeConverterFor(Object.class);
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf("1", Number.class);
                }
            }, should.raise(TargetTypeNotSupportedException.class));
        }

        public void shouldVerifyOnFirstStageThatTheConvertedValueIsAnInstanceOfTheTargetClass() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(returnValue(new Object()));
                one (subConverter  ).valueOf("1", Number.class); will(returnValue(new BigInteger("1")));
            }});
            specify(provider.valueOf("1", Number.class), should.equal(new BigInteger("1")));
        }

        public void shouldVerifyOnSecondStageThatTheConvertedValueIsAnInstanceOfTheTargetClass() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(returnValue(new Object()));
                one (subConverter  ).valueOf("1", Number.class); will(returnValue(new Object()));
                one (superConverter).valueOf("1", Number.class); will(returnValue(new BigInteger("1")));
            }});
            specify(provider.valueOf("1", Number.class), should.equal(new BigInteger("1")));
        }

        public void shouldVerifyOnThirdStageThatTheConvertedValueIsAnInstanceOfTheTargetClass() throws ConversionFailedException {
            checking(new Expectations() {{
                one (exactConverter).valueOf("1", Number.class); will(returnValue(new Object()));
                one (subConverter  ).valueOf("1", Number.class); will(returnValue(new Object()));
                one (superConverter).valueOf("1", Number.class); will(returnValue(new Object()));
            }});
            specify(new Block() {
                public void run() throws Throwable {
                    provider.valueOf("1", Number.class);
                }
            }, should.raise(TargetTypeNotSupportedException.class));
        }
    }
}
