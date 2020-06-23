package WebApi;

public class HttpResponse {
    private String content, applicationType;

    HttpResponse(String applicationType, String content){
        this.content = content;
        this.applicationType = applicationType;
    }

    public String getContent() {
        return content;
    }

    public String getApplicationType() {
        return applicationType;
    }
}
