package framework.model;

/**
 * 反思评价结果
 * 
 * 用于 Reflexion Agent 的评价结果
 */
public class ReflectionEvaluation {
    private boolean isSatisfactory;  // 是否满意
    private String evaluationReason;  // 评价原因
    private String improvementSuggestions;  // 改进建议
    
    public ReflectionEvaluation(boolean isSatisfactory, String evaluationReason, String improvementSuggestions) {
        this.isSatisfactory = isSatisfactory;
        this.evaluationReason = evaluationReason != null ? evaluationReason : "";
        this.improvementSuggestions = improvementSuggestions != null ? improvementSuggestions : "";
    }
    
    public boolean isSatisfactory() {
        return isSatisfactory;
    }
    
    public void setSatisfactory(boolean satisfactory) {
        isSatisfactory = satisfactory;
    }
    
    public String getEvaluationReason() {
        return evaluationReason;
    }
    
    public void setEvaluationReason(String evaluationReason) {
        this.evaluationReason = evaluationReason;
    }
    
    public String getImprovementSuggestions() {
        return improvementSuggestions;
    }
    
    public void setImprovementSuggestions(String improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }
}

