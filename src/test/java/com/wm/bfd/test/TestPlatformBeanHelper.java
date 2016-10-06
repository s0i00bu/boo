package com.wm.bfd.test;

import com.wm.bfd.oo.yaml.PlatformBean;
import com.wm.bfd.oo.yaml.helper.PlatformBeanHelper;

import com.oo.api.exception.OneOpsClientAPIException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPlatformBeanHelper extends BfdOoTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestPlatformBeanHelper.class);

  @Test
  public void testGetPlatforms() throws OneOpsClientAPIException {
    List<PlatformBean> list = PlatformBeanHelper.getPlatforms(this.config.getYaml().getPlatforms());
    LOG.debug("Total {} platforms.", list.size());
  }

}
