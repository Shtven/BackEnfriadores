package com.codespace.simulator.audit;

import com.codespace.simulator.models.TemperaturaEvent;
import com.codespace.simulator.services.AlarmaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Component
public class AuditThread implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditThread.class);

    @Autowired
    private AlarmaService alarmaService;

    private final BlockingQueue<TemperaturaEvent> auditQueue;
    private final AuditService auditService;  //  la transacción al @Service

    public AuditThread(BlockingQueue<TemperaturaEvent> auditQueue,
                       AuditService auditService) {
        this.auditQueue   = auditQueue;
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(">>> AuditThread iniciado");
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TemperaturaEvent event = auditQueue.take();
                    auditService.procesarEvento(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("AuditThread interrumpido");
                }
            }
        });
        thread.setName("audit-thread");
        thread.setDaemon(true);
        thread.start();
    }
}