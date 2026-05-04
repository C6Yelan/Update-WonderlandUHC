package org.mcwonderland.uhc.bootstrap;

import org.mcwonderland.uhc.Dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class DependencyReport {

    private final Map<Dependency, Entry> entries = new EnumMap<>(Dependency.class);

    public void markAvailable(Dependency dependency) {
        entries.put(dependency, new Entry(dependency, true, false, ""));
    }

    public void markUnavailable(Dependency dependency, String reason) {
        entries.put(dependency, new Entry(dependency, false, false, reason));
    }

    public void markDisabled(Dependency dependency, String reason) {
        entries.put(dependency, new Entry(dependency, false, true, reason));
    }

    public Collection<Entry> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public boolean isAvailable(Dependency dependency) {
        Entry entry = entries.get(dependency);
        return entry != null && entry.isAvailable();
    }

    public static final class Entry {
        private final Dependency dependency;
        private final boolean available;
        private final boolean disabled;
        private final String reason;

        private Entry(Dependency dependency, boolean available, boolean disabled, String reason) {
            this.dependency = dependency;
            this.available = available;
            this.disabled = disabled;
            this.reason = reason;
        }

        public Dependency getDependency() {
            return dependency;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public String getReason() {
            return reason;
        }
    }
}
