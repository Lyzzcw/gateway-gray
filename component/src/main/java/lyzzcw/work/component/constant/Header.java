package lyzzcw.work.component.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/28 9:22
 * Description: No Description
 */
@Getter
@AllArgsConstructor
public enum Header {

    VERSION("version"),

    TAG("tag");

    private String value;
}
