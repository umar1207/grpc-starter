package com.freemanan.starter.grpc.server.feature.exceptionhandling;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Freeman
 */
public class ExceptionHandlerListener<ReqT, RespT> extends SimpleForwardingServerCallListener<ReqT> {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerListener.class);
    private final ServerCall<ReqT, RespT> call;
    private final List<ExceptionHandler> exceptionHandlers;
    private final List<UnhandledExceptionProcessor> unhandledExceptionProcessors;

    protected ExceptionHandlerListener(
            ServerCall.Listener<ReqT> delegate,
            ServerCall<ReqT, RespT> call,
            List<ExceptionHandler> exceptionHandlers,
            List<UnhandledExceptionProcessor> unhandledExceptionProcessors) {
        super(delegate);
        this.call = call;
        this.exceptionHandlers = exceptionHandlers;
        this.unhandledExceptionProcessors = unhandledExceptionProcessors;
    }

    @Override
    public void onMessage(ReqT message) {
        try {
            super.onMessage(message);
        } catch (Throwable e) {
            if (!handle(e)) {
                throw e;
            }
        }
    }

    @Override
    public void onHalfClose() {
        try {
            super.onHalfClose();
        } catch (Throwable e) {
            if (!handle(e)) {
                throw e;
            }
        }
    }

    private boolean handle(Throwable e) {
        for (ExceptionHandler handler : exceptionHandlers) {
            if (handler.support(e)) {
                StatusRuntimeException sre = handler.handle(e);
                if (sre.getStatus() == null) {
                    log.warn(
                            "Should NOT return 'null' status, skip ExceptionHandler: {}",
                            handler.getClass().getSimpleName());
                    continue;
                }
                if (sre.getStatus().isOk()) {
                    log.warn(
                            "Should NOT return 'OK' status while handling exception, skip ExceptionHandler: {}",
                            handler.getClass().getSimpleName());
                    continue;
                }
                call.close(
                        sre.getStatus(), Optional.ofNullable(sre.getTrailers()).orElseGet(Metadata::new));
                return true;
            }
        }
        unhandledExceptionProcessors.forEach(processor -> processor.process(e));
        return false;
    }
}
