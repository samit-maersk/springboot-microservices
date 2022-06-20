package com.samitkumarpatel.springbootms;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ReactiveTest {

    /*@Test
    void onErrorResumeTest() {
        Flux.just(1,2,0,3)
                .map(i -> 100/i)
                .onErrorReturn(10)
                .subscribe(System.out::println);
    }*/
}
