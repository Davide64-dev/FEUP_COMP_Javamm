package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class JasminTest {

    @Test
    public void ollirToJasminBasic() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminBasic.ollir");
    }

    @Test
    public void ollirToJasminArithmetics() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminArithmetics.ollir");
    }

    @Test
    public void ollirToJasminInvoke() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminInvoke.ollir");
    }

    @Test
    public void ollirToJasminFields() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminFields.ollir");
    }

    @Test
    public void ollirHelloWorld() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirHelloWorld.ollir");
    }

    @Test
    public void ollirSimple() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirSimple.ollir");
    }

    @Test
    public void helloWorldIfNotFalse() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/HelloWorldIfNotFalse.ollir");
    }

    @Test
    public void helloWorldWhileLT5() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/HelloWorldWhileLT5.ollir");
    }

    public static void testOllirToJasmin(String resource, String expectedOutput) {
        JasminResult result = null;

        // If AstToJasmin pipeline, change name of the resource and execute other test
        // we're not doing this one
        if (TestUtils.hasAstToJasminClass()) {

            // Rename resource
            var jmmResource = SpecsIo.removeExtension(resource) + ".jmm";

            // Test Jmm resource
            result = TestUtils.backend(SpecsIo.getResource(jmmResource));

        } else { // for testing ollir to jasmin, which is the one we're doing

            var ollirResult = new OllirResult(SpecsIo.getResource(resource), Collections.emptyMap());
            result = TestUtils.backend(ollirResult);
        }


        var testName = new File(resource).getName();
        System.out.println(testName + ":\n" + result.getJasminCode());
        var runOutput = result.runWithFullOutput();
        assertEquals("Error while running compiled Jasmin: " + runOutput.getOutput(), 0, runOutput.getReturnValue());

        if (expectedOutput != null) {
            assertEquals(expectedOutput, runOutput.getOutput());
        }
    }

    public static void testOllirToJasmin(String resource) {
        testOllirToJasmin(resource, null);
    }
}
