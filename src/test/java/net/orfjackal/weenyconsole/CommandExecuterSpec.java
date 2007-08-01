package net.orfjackal.weenyconsole;

import jdave.Block;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 31.7.2007
 */
@RunWith(JDaveRunner.class)
public class CommandExecuterSpec extends Specification<Object> {

    public class CommandsWithNoParameters {

        private class TargetMock {
            private int fooExecuted = 0;

            public void foo() {
                fooExecuted++;
            }
        }

        private TargetMock target;
        private CommandExecuter exec;

        public Object create() {
            target = new TargetMock();
            exec = new CommandExecuter(target);
            return null;
        }

        public void shouldCallAMethodWithTheSameNameAsTheCommand() {
            exec.execute("foo");
            specify(target.fooExecuted, should.equal(1));
        }

        public void shouldRaiseAnExceptionOnAnUnknownCommand() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("bar");
                }
            }, should.raise(CommandNotFoundException.class, "command not found: bar"));
            specify(target.fooExecuted, should.equal(0));
        }

        public void anEmptyCommandShouldExitSilently() {
            exec.execute("");               // trivial case of an empty string
            exec.execute("  \n");           // whitespace should also be regarded as an empty command
            specify(target.fooExecuted, should.equal(0));
        }
    }

    public class CommandsWithStringParameters {

        private class TargetMock {
            private String fooParameter;
            private String barParameter1;
            private String barParameter2;

            public void foo(String s) {
                fooParameter = s;
            }

            public void bar(String s1, String s2) {
                barParameter1 = s1;
                barParameter2 = s2;
            }
        }

        private TargetMock target;
        private CommandExecuter exec;

        public Object create() {
            target = new TargetMock();
            exec = new CommandExecuter(target);
            return null;
        }

        public void shouldSupportTheUseOfAParameter() {
            exec.execute("foo x");
            specify(target.fooParameter, should.equal("x"));
        }

        public void shouldSupportTheUseOfMultipleParameters() {
            exec.execute("bar y z");
            specify(target.barParameter1, should.equal("y"));
            specify(target.barParameter2, should.equal("z"));
        }

        public void shouldNotCareAboutEmptySpaceBetweenWords() {
            exec.execute(" bar y  z ");
            specify(target.barParameter1, should.equal("y"));
            specify(target.barParameter2, should.equal("z"));
        }

        public void shouldSupportMultipleWordsInDoubleQuotes() {
            exec.execute("foo \"two words\"");
            specify(target.fooParameter, should.equal("two words"));
        }

        public void shouldSupportMultipleWordsUsingTheEscapeCharacter() {
            exec.execute("foo two\\ words");
            specify(target.fooParameter, should.equal("two words"));
        }

        public void shouldSupportMixingDoubleQuotesAndTheEscapeCharacter() {
            exec.execute("foo \"escape char is \\\\ and quote char is \\\"\"");
            specify(target.fooParameter, should.equal("escape char is \\ and quote char is \""));
        }

        public void shouldSupportNewlineCharacter() {
            exec.execute("foo line1\\nline2");
            specify(target.fooParameter, should.equal("line1\nline2"));
        }

        public void shouldSupportTabCharacter() {
            exec.execute("foo col1\\tcol2");
            specify(target.fooParameter, should.equal("col1\tcol2"));
        }

        public void shouldSupportNullAsAParameter() {
            target.fooParameter = "initial value";
            exec.execute("foo \\0");
            specify(target.fooParameter, should.equal(null));
        }

        public void shouldNotAllowNullAsPartOfAWord() {
            target.fooParameter = "initial value";
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("foo aa\\0aa");
                }
            }, should.raise(MalformedCommandException.class, "" +
                    "null not allowed here: foo aa\\0aa\n" +
                    "                              ^"));
            specify(target.fooParameter, should.equal("initial value"));
        }

        public void shouldComplainAboutMissingDoubleQuotes() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("foo \"not properly quoted");
                }
            }, should.raise(MalformedCommandException.class, "" +
                    "double quote expected: foo \"not properly quoted\n" +
                    "                                               ^"));
        }

        public void shouldComplainAboutMissingEscapeSequences() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("foo \\e_is_not_a_valid_escape_sequence");
                }
            }, should.raise(MalformedCommandException.class, "" +
                    "escape sequence expected: foo \\e_is_not_a_valid_escape_sequence\n" +
                    "                               ^"));
        }

        public void shouldComplainAboutIncompleteEscapeSequences() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("foo escape_char_has_nothing_to_escape\\");
                }
            }, should.raise(MalformedCommandException.class, "" +
                    "escape sequence expected: foo escape_char_has_nothing_to_escape\\\n" +
                    "                                                                ^"));
        }
    }

    public class CommandsWithNumericParameters {

        private class TargetMock {
            private Integer integerValue;
            private Double doubleValue;
            private Boolean primBoolean;
            private Byte primByte;
            private Character primChar;
            private Short primShort;
            private Integer primInt;
            private Long primLong;
            private Float primFloat;
            private Double primDouble;
            private Integer nullCheckValue;

            public void integer(Integer x) {
                integerValue = x;
            }

            public void double_(Double x) {
                doubleValue = x;
            }

            public void primitives(boolean boolean_, byte byte_, char char_, short short_,
                                   int int_, long long_, float float_, double double_) {
                primBoolean = boolean_;
                primByte = byte_;
                primChar = char_;
                primShort = short_;
                primInt = int_;
                primLong = long_;
                primFloat = float_;
                primDouble = double_;
            }

            public void nullCheck(int x) {
                nullCheckValue = x;
            }
        }

        private TargetMock target;
        private CommandExecuter exec;

        public Object create() {
            target = new TargetMock();
            exec = new CommandExecuter(target);
            return null;
        }

        public void shouldSupportTheUseOfIntegerObjects() {
            exec.execute("integer 1");
            specify(target.integerValue, should.equal(1));
        }

        public void shouldSupportTheUseOfDoubleObjects() {
            exec.execute("double_ 2.5");
            specify(target.doubleValue, should.equal(2.5));
        }

        public void shouldSupportTheUseOfAllPrimitiveTypes() {
            exec.execute("primitives true -128 c 32767 2147483647 9223372036854775807 1.123 2.456");
            specify(target.primBoolean, should.equal(Boolean.TRUE));
            specify(target.primByte, should.equal(Byte.MIN_VALUE));
            specify(target.primChar, should.equal('c'));
            specify(target.primShort, should.equal(Short.MAX_VALUE));
            specify(target.primInt, should.equal(Integer.MAX_VALUE));
            specify(target.primLong, should.equal(Long.MAX_VALUE));
            specify(target.primFloat, should.equal(1.123F));
            specify(target.primDouble, should.equal(2.456));
        }

        public void shouldNotAllowConvertingNullToAPrimitiveType() {
            target.nullCheckValue = 123;
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("null check \\0");
                }
            }, should.raise(CommandNotFoundException.class));
            specify(target.nullCheckValue, should.equal(123));
        }
    }

    public class MultiWordCommands {

        private class TargetMock {
            private int methodOneExecuted;
            private int methodOneMoreExecuted;
            private int methodTwoValue;

            public void methodOne() {
                methodOneExecuted++;
            }

            public void methodOneMore() {
                methodOneMoreExecuted++;
            }

            public void methodTwo(int x) {
                methodTwoValue = x;
            }
        }

        private TargetMock target;
        private CommandExecuter exec;

        public Object create() {
            target = new TargetMock();
            exec = new CommandExecuter(target);
            return null;
        }

        public void shouldCallAMethodWithAllTheWordsInItsName() {
            exec.execute("method one");
            specify(target.methodOneExecuted, should.equal(1));
        }

        public void shouldSupportTheUseOfParameters() {
            exec.execute("method two 42");
            specify(target.methodTwoValue, should.equal(42));
        }

        public void shouldSupportOverloadingOfCommands() {
            exec.execute("method one more");
            specify(target.methodOneMoreExecuted, should.equal(1));
        }
    }

    // TODO: support for enum classes

    public class CommandsWithPossiblyConflictingNamesAndParameters {

        private class TargetMock {
            private int oneExecuted;
            private String oneValue;
            private int oneMoreExecuted;
            private Integer constructorErrorValue;
            private Integer overloadedInt;
            private Boolean overloadedBoolean;
            private Boolean booleanCaseValue;
            private int nullCheckExecuted;

            public void one(String more) {
                oneExecuted++;
                oneValue = more;
            }

            public void oneMore() {
                oneMoreExecuted++;
            }

            public void constructorError(int x) {
                constructorErrorValue = x;
            }

            public void overloaded(int x) {
                overloadedInt = x;
            }

            public void overloaded(boolean x) {
                overloadedBoolean = x;
            }

            public void booleanCase(boolean x) {
                booleanCaseValue = x;
            }

            public void nullChecknull() {
                nullCheckExecuted++;
            }

            public void nullCheckNull() {
                nullCheckExecuted++;
            }
        }

        private TargetMock target;
        private CommandExecuter exec;

        public Object create() {
            target = new TargetMock();
            exec = new CommandExecuter(target);
            return null;
        }

        public void shouldPrioritizeTheMethodWithTheLongestName() {
            exec.execute("one more");
            specify(target.oneMoreExecuted, should.equal(1));
            specify(target.oneExecuted, should.equal(0));
            specify(target.oneValue, should.equal(null));
        }

        /**
         * The constructor {@link Integer#Integer(String)} will throw
         * an exception if the string can not be converted to an integer.
         * The program must handle silently all exceptions thrown by a constructor.
         */
        public void shouldNotCallMethodsToWhichTheParameterCanNotBeConverted() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("constructor error not_int");
                }
            }, should.raise(CommandNotFoundException.class));
            specify(target.constructorErrorValue, should.equal(null));
        }

        public void shouldChooseFromOverloadedMethodsTheOneToWhichTheParametersCanBeConvertedV1() {
            exec.execute("overloaded 42");
            specify(target.overloadedInt, should.equal(42));
            specify(target.overloadedBoolean, should.equal(null));
        }

        public void shouldChooseFromOverloadedMethodsTheOneToWhichTheParametersCanBeConvertedV2() {
            exec.execute("overloaded true");
            specify(target.overloadedInt, should.equal(null));
            specify(target.overloadedBoolean, should.equal(true));
        }

        /**
         * Boolean's constructor would convert "foo" to FALSE, but
         * we want to have stricter conversions, especially if there
         * would be an overloaded method with more exact parameters.
         */
        public void shouldAllowCreatingABooleanOnlyFromTrueOrFalse() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("boolean case not_boolean");
                }
            }, should.raise(CommandNotFoundException.class));
            specify(target.booleanCaseValue, should.equal(null));
        }

        public void shouldNotMistakeNullValuesToBePartOfTheMethodName() {
            specify(new Block() {
                public void run() throws Throwable {
                    exec.execute("null check \\0");
                }
            }, should.raise(CommandNotFoundException.class));
            specify(target.nullCheckExecuted, should.equal(0));
        }

        // TODO: handle the case when the target method throws an exception
        // TODO: overloaded methods with different number of parameters
        // TODO: should not call equals() and other unwanted methods from superclasses (use a marker interface)
        // TODO: support for varargs
    }
}

