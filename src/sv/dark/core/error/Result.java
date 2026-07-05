package sv.dark.core.error;

/**
 * Valhalla-Ready Result Pattern.
 * Replaces Exceptions for flow-control in hot-paths.
 * Since it is a record, Java 26+ can flatten this on the stack.
 */
public record Result<T>(T value, ErrorCode error) {

    public static <T> Result<T> success(T value) {
        return new Result<>(value, ErrorCode.NONE);
    }

    public static <T> Result<T> failure(ErrorCode error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == ErrorCode.NONE;
    }
    
    public boolean isFailure() {
        return error != ErrorCode.NONE;
    }
}
