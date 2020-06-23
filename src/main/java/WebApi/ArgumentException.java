package WebApi;

public class ArgumentException extends Exception {
    ArgumentException(String errorMsg){
        super(errorMsg);
    }
}
