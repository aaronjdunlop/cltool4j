package cltool4j;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import cltool4j.args4j.TestArgs4J;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestArgs4J.class, TestBaseCommandlineTool.class, TestLinewiseCommandlineTool.class })
public class AllToolTests {
}
