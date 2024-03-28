package com.zyq.chirp.common.domain.model;

import com.zyq.chirp.common.domain.enums.OrderEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class QueryDto {
    protected final static Integer DEFAULT_PAGE = 1;
    protected final static Integer DEFAULT_PAGE_SIZE = 20;
    protected final static String DEFAULT_ORDER = OrderEnum.DESC.toString();
    protected String keyword;
    protected Integer page = DEFAULT_PAGE;
    @Length(max = 100)
    protected Integer pageSize = DEFAULT_PAGE_SIZE;
    protected String order = OrderEnum.DESC.toString();

    public void withDefault() {
        this.page = Optional.ofNullable(this.page).orElse(DEFAULT_PAGE);
        this.pageSize = Optional.ofNullable(this.pageSize).orElse(DEFAULT_PAGE_SIZE);
        this.order = Optional.ofNullable(this.order).orElse(DEFAULT_ORDER);
    }
}
