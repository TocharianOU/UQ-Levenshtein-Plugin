package org.elasticsearch.example.expertscript;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.DocReader;
import org.elasticsearch.script.DocValuesDocReader;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScoreScript.LeafFactory;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.ScriptFactory;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class UQLevenshteinPlugin extends Plugin implements ScriptPlugin {

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new MyExpertScriptEngine();
    }

    private static class MyExpertScriptEngine implements ScriptEngine {
        @Override
        public String getType() {
            return "similarity_costum_scripts";
        }

        @Override
        public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> params) {
            if (!context.equals(ScoreScript.CONTEXT)) {
                throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
            }

            if ("uq_levenshtein_score".equals(scriptSource)) {
                ScoreScript.Factory factory = new LevenshteinScoreFactory();
                return context.factoryClazz.cast(factory);
            }
            throw new IllegalArgumentException("Unknown script name " + scriptSource);
        }

        @Override
        public void close() {
           
        }

        @Override
        public Set<ScriptContext<?>> getSupportedContexts() {
            return Set.of(ScoreScript.CONTEXT);
        }

        private static class LevenshteinScoreFactory implements ScoreScript.Factory, ScriptFactory {
            @Override
            public boolean isResultDeterministic() {
                return true;
            }

            @Override
            public LeafFactory newFactory(Map<String, Object> params, SearchLookup lookup) {
                return new LevenshteinScoreLeafFactory(params, lookup);
            }
        }

        private static class LevenshteinScoreLeafFactory implements LeafFactory {
            private final Map<String, Object> params;
            private final SearchLookup lookup;
            private final String field;
            private final String term;
            private final double alpha;
            private final double beta;
            private final int maxDist;

            private LevenshteinScoreLeafFactory(Map<String, Object> params, SearchLookup lookup) {
                if (!params.containsKey("field")) {
                    throw new IllegalArgumentException("Missing parameter [field]");
                }
                if (!params.containsKey("term")) {
                    throw new IllegalArgumentException("Missing parameter [term]");
                }
                this.params = params;
                this.lookup = lookup;
                this.field = params.get("field").toString();
                this.term = params.get("term").toString();
                this.alpha = params.containsKey("alpha") ? Double.parseDouble(params.get("alpha").toString()) : 0.5;
                this.beta = params.containsKey("beta") ? Double.parseDouble(params.get("beta").toString()) : 0.5;
                this.maxDist = params.containsKey("max_dist") ? Integer.parseInt(params.get("max_dist").toString()) : 3;
            }

            @Override
            public boolean needs_score() {
                return false;
            }

            @Override
            public ScoreScript newInstance(DocReader docReader) {
                return new ScoreScript(params, lookup, docReader) {
                    @Override
                    public double execute(ExplanationHolder explanation) {
                        try {
                            Map<String, ScriptDocValues<?>> doc = ((DocValuesDocReader) docReader).doc();
                            ScriptDocValues<?> docValues = doc.get(field);
                            String docValue = docValues != null && !docValues.isEmpty() ? docValues.get(0).toString() : "";

                            double similarity = SimilarityUtils.mixedSimilarity(term, docValue, alpha, beta, maxDist);

                            if (explanation != null) {
                                explanation.set("Custom similarity score based on Levenshtein and Jaccard.");
                            }
                            return similarity;
                        } catch (Exception e) {
                            throw new RuntimeException("Error retrieving field value", e);
                        }
                    }
                };
            }
        }
    }
}
