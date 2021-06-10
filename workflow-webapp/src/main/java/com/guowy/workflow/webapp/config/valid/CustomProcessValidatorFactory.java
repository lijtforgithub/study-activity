package com.guowy.workflow.webapp.config.valid;

import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorFactory;
import org.activiti.validation.validator.ValidatorSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author LiJingTang
 * @date 2020-05-14 16:17
 */
@Component
public class CustomProcessValidatorFactory extends ProcessValidatorFactory {

    @Autowired
    private CustomProcessValidator customProcessValidator;

    @Override
    public ProcessValidator createDefaultProcessValidator() {
        ProcessValidator processValidator = super.createDefaultProcessValidator();
        //将自定义校验器添加进去
        ValidatorSet validatorSet = new ValidatorSet("自定义校验规则");
        validatorSet.addValidator(customProcessValidator);

        processValidator.getValidatorSets().add(validatorSet);
        return processValidator;
    }

}
