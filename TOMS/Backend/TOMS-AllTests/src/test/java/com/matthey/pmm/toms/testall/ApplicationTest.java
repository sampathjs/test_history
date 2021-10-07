package com.matthey.pmm.toms.testall;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.service.mock.MockApplication;

/*
 * History:
 * 2021-10-06	V1.0	jwaechter	- Initial Version
 */

/**
 * 
 * @author jwaechter
 * @version 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes={MockApplication.class})
@AutoConfigureTestDatabase(replace=Replace.NONE)
public class ApplicationTest {

  @Test
  public void contextLoads() {
	  System.out.println("contextLoads test successful");
  }
}
