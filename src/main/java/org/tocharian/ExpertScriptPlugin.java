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

public class ExpertScriptPlugin extends Plugin implements ScriptPlugin {

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new MyExpertScriptEngine();
    }

    private static class MyExpertScriptEngine implements ScriptEngine {
        @Override
        public String getType() {
            return "similarity_custom";
        }

        @Override
        public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> params) {
            if (!context.equals(ScoreScript.CONTEXT)) {
                throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
            }

            if ("uq_score".equals(scriptSource)) {
                ScoreScript.Factory factory = new LevenshteinScoreFactory();
                return context.factoryClazz.cast(factory);
            }
            throw new IllegalArgumentException("Unknown script name " + scriptSource);
        }

        @Override
        public void close() {
            // 可选的关闭资源
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
            private final int windowSize;
            private final int from;

            // 初始化，增加 window_size 和 from 参数
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
                this.windowSize = params.containsKey("window_size") ? Integer.parseInt(params.get("window_size").toString()) : Integer.MAX_VALUE; // 默认无限大
                this.from = params.containsKey("from") ? Integer.parseInt(params.get("from").toString()) : 0; // 默认从 0 开始
            }

            @Override
            public boolean needs_score() {
                return true;  // 需要访问原始评分
            }

            @Override
            public ScoreScript newInstance(DocReader docReader) {
                return new ScoreScript(params, lookup, docReader) {
                    private int docCounter = 0; // 计数器，用于 window_size 和 from 控制

                    @Override
                    public double execute(ExplanationHolder explanation) {
                        docCounter++; // 每次执行文档评分时计数

                        // 如果文档计数小于 `from`，跳过评分，直接返回原始分数
                        if (docCounter < from) {
                            return get_score();  // `from` 之前的文档返回原始分数
                        }

                        // 如果超出 window_size，跳过评分，直接返回原始分数
                        if (docCounter >= from + windowSize) {
                            return get_score();  // 超出 `window_size` 的文档返回原始分数
                        }

                        try {
                            Map<String, ScriptDocValues<?>> doc = ((DocValuesDocReader) docReader).doc();
                            ScriptDocValues<?> docValues = doc.get(field);
                            String docValue = docValues != null && !docValues.isEmpty() ? docValues.get(0).toString() : "";

                            // 计算自定义相似度
                            double similarity = SimilarityUtils.mixedSimilarity(term, docValue, alpha, beta, maxDist);

                            // 获取文档的原始评分
                            double originalScore = get_score();

                            // 相似度分数加 1 后再乘以原始分数，确保基础评分保留
                            double finalScore = (1 + similarity) * originalScore;

                            // 设置解释说明
                            if (explanation != null) {
                                explanation.set("Custom similarity score added with 1, then multiplied by original document score.");
                            }

                            return finalScore;  // 返回调整后的分数
                        } catch (Exception e) {
                            throw new RuntimeException("Error retrieving field value", e);
                        }
                    }
                };
            }
        }
    }
}
