package org.brylex.krekar.file;

import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;
import akka.util.Duration;
import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class FileJallaTest {

    private final Path inputPath = Paths.get("target/input");

    @Before
    public void setUp() throws Exception {
        deleteDirectory(inputPath);
        Files.createDirectories(inputPath);
    }

    @Test
    public void testName() throws Exception {

        ActorSystem system = ActorSystem.create("KrekarSystem");

        final Output<InputStream> streamOutput = TypedActor.get(system).typedActorOf(new TypedProps<StreamOutput>(Output.class, new Creator<StreamOutput>() {
            @Override
            public StreamOutput create() throws Exception {
                return new StreamOutput(System.out);
            }
        }), "streamOutput");

        TypedActor.get(system).typedActorOf(new TypedProps<FileInput>(PollingInput.class, new Creator<FileInput>() {
            @Override
            public FileInput create() throws Exception {
                return new FileInput(inputPath, streamOutput);
            }
        }), "fileInput");

        Thread.sleep(Long.MAX_VALUE);
    }

    public static class FileInputDefinition {

    }

    public static interface Input<T> {

    }

    public static interface Output<T> {
        void output(T output);
    }

    public static interface PollingInput<T> extends Input<T> {

        public static class Signal {

            private final long timestamp;

            public Signal(long timestamp) {
                this.timestamp = timestamp;
            }
        }

        void poll(Signal signal);

    }

    public static abstract class AbstractPollingInput<T> implements PollingInput<T>, TypedActor.PreStart {

        @Override
        public void preStart() {

            ActorSystem system = TypedActor.context().system();

            Scheduler scheduler = system.scheduler();
            final PollingInput<Path> pollingInput = TypedActor.self();
            scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.SECONDS), new Runnable() {
                @Override
                public void run() {
                    pollingInput.poll(new Signal(System.currentTimeMillis()));
                }
            });
        }

    }

    public static class FileInput extends AbstractPollingInput<Path> {

        private final Logger logger = LoggerFactory.getLogger(FileInput.class);

        private final Path dir;
        private final Output<InputStream> output;

        public FileInput(final Path dir, final Output<InputStream> output) {
            this.dir = dir;
            this.output = output;
        }

        @Override
        public void poll(Signal signal) {

            try {

                Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path visitedDir, BasicFileAttributes attrs) throws IOException {

                        if (dir.equals(visitedDir)) {
                            return FileVisitResult.CONTINUE;
                        }

                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                        lock(file);

                        Path workFilePath = getWorkFilePath(file);
                        output.output(Files.newInputStream(workFilePath));

                        return FileVisitResult.CONTINUE;
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException("Unable to process files in path [" + dir + "]", e);
            }
        }

        void lock(Path path) {

            logger.info("Going to lock file [" + path + "]");

            Path workFile = getWorkFilePath(path);
            Path workDir = workFile.getParent();
            if (!Files.exists(workDir)) {
                try {
                    Files.createDirectory(workDir);
                    logger.info("Created work directory [" + workDir + "].");
                } catch (IOException e) {
                    throw new RuntimeException("Unable to create work directory [" + workDir + "].", e);
                }
            }

            try {
                Files.move(path, workFile);
                logger.info(Thread.currentThread() + " :: Locked file [" + path + "].");
            } catch (IOException e) {
                throw new RuntimeException("Unable to move [" + path + "] to path [" + workFile + "].", e);
            }
        }

        private Path getWorkFilePath(Path path) {

            Path workDir = path.toAbsolutePath().getParent().resolve(".wrk");

            return workDir.resolve(path.getFileName().toString());
        }
    }

    public static class StreamOutput implements Output<InputStream> {

        private final Logger logger = LoggerFactory.getLogger(StreamOutput.class);

        private final OutputStream outputStream;

        public StreamOutput(OutputStream outputStream) {
            this.outputStream = new BufferedOutputStream(outputStream);
        }

        @Override
        public void output(final InputStream input) {
            try {
                ByteStreams.copy(input, outputStream);
                outputStream.flush();
                logger.info("Successfully copied content to output stream.");
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy stream to output.", e);
            }
        }
    }

    public static interface Processor<T> {

        Object process(T input);
    }

    public static interface Transformer<I, U> {

        U transform(I input);
    }

    private void deleteDirectory(Path dirToDelete) throws IOException {
        if (Files.isDirectory(dirToDelete)) {
            Files.walkFileTree(dirToDelete, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }
            });
            Files.deleteIfExists(dirToDelete);
        }
    }


}
