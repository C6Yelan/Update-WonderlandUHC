package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Compatibility shim for legacy SimpleReplacer usages in WonderlandUHC.
 * Delegates actual placeholder replacement to Foundation v6 Replacer.
 */
public class SimpleReplacer implements Cloneable {

    private final List<String> messages;
    private final List<Object> replacements = new ArrayList<>();

    /** Legacy: accept a single message line. */
    public SimpleReplacer(final String message) {
        this(message == null ? Collections.emptyList() : Collections.singletonList(message));
    }

    /** Legacy: accept multiple message lines. */
    public SimpleReplacer(final List<String> messages) {
        this.messages = messages == null ? Collections.emptyList() : new ArrayList<>(messages);
    }

    /** Add one key-value replacement. */
    public SimpleReplacer replace(final String key, final Object value) {
        if (key == null) return this;
        this.replacements.add(key);
        this.replacements.add(value);
        return this;
    }

    /**
     * Legacy overload: replace a placeholder with a joined list.
     * Example: replace("{materials}", triggerBlocks, " - ")
     */
    public SimpleReplacer replace(final String key, final Iterable<?> values, final String delimiter) {
        if (key == null) return this;

        final StringJoiner joiner = new StringJoiner(delimiter == null ? "" : delimiter);
        if (values != null) {
            for (final Object v : values) {
                if (v != null) joiner.add(String.valueOf(v));
            }
        }
        return replace(key, joiner.toString());
    }

    /** Legacy helper used by multiple scenarios. */
    public SimpleReplacer replaceTime(final Number time) {
        // 先用最保守做法：提供常見 placeholder，不影響編譯，也方便後續依 messages.yml 再微調
        return this
                .replace("{time}", time)
                .replace("{seconds}", time)
                .replace("{second}", time);
    }

    /** Add replacements in pairs: key1, value1, key2, value2... */
    public SimpleReplacer replaceArray(final Object... pairs) {
        if (pairs != null) {
            Collections.addAll(this.replacements, pairs);
        }
        return this;
    }

    /** Build replaced lines. */
    public List<String> buildList() {
        final List<String> base = new ArrayList<>(this.messages);

        if (this.replacements.isEmpty()) {
            return base;
        }

        return Replacer.replaceArray(base, this.replacements.toArray());
    }

    /** Legacy API: returns single String (1 line or joined by \n). */
    public String getMessages() {
        final List<String> built = buildList();
        if (built.isEmpty()) return "";

        if (built.size() == 1) return Objects.toString(built.get(0), "");

        final StringJoiner joiner = new StringJoiner("\n");
        for (final String line : built) {
            joiner.add(Objects.toString(line, ""));
        }
        return joiner.toString();
    }

    /** Legacy API: returns String[] for Chat.send(...). */
    public String[] toArray() {
        final List<String> built = buildList();
        return built.toArray(new String[0]);
    }

    @Override
    public SimpleReplacer clone() {
        final SimpleReplacer copy = new SimpleReplacer(this.messages);
        copy.replacements.addAll(this.replacements);
        return copy;
    }
}