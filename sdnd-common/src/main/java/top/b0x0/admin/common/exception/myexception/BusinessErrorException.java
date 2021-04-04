package top.b0x0.admin.common.exception.myexception;

import top.b0x0.admin.common.util.enums.BusinessMsgEnum;

/**
 * 自定义业务异常
 *
 * @author ling
 */
public class BusinessErrorException extends RuntimeException {

    private static final long serialVersionUID = -7480022450501760611L;

    /**
     * 异常码
     */
    private Integer code;

    /**
     * 异常提示信息
     */
    private String message;

    public BusinessErrorException(String msg) {
        this.code = 500;
        this.message = msg;
    }

    public BusinessErrorException(BusinessMsgEnum businessMsgEnum) {
        this.code = businessMsgEnum.code();
        this.message = businessMsgEnum.msg();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}