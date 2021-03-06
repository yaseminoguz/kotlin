// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

/**
 * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.StringTable}
 */
public final class StringTable extends
    org.jetbrains.kotlin.protobuf.GeneratedMessageLite implements
    // @@protoc_insertion_point(message_implements:org.jetbrains.kotlin.backend.common.serialization.proto.StringTable)
    StringTableOrBuilder {
  // Use StringTable.newBuilder() to construct.
  private StringTable(org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder builder) {
    super(builder);
    this.unknownFields = builder.getUnknownFields();
  }
  private StringTable(boolean noInit) { this.unknownFields = org.jetbrains.kotlin.protobuf.ByteString.EMPTY;}

  private static final StringTable defaultInstance;
  public static StringTable getDefaultInstance() {
    return defaultInstance;
  }

  public StringTable getDefaultInstanceForType() {
    return defaultInstance;
  }

  private final org.jetbrains.kotlin.protobuf.ByteString unknownFields;
  private StringTable(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    initFields();
    int mutable_bitField0_ = 0;
    org.jetbrains.kotlin.protobuf.ByteString.Output unknownFieldsOutput =
        org.jetbrains.kotlin.protobuf.ByteString.newOutput();
    org.jetbrains.kotlin.protobuf.CodedOutputStream unknownFieldsCodedOutput =
        org.jetbrains.kotlin.protobuf.CodedOutputStream.newInstance(
            unknownFieldsOutput);
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(input, unknownFieldsCodedOutput,
                                   extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            org.jetbrains.kotlin.protobuf.ByteString bs = input.readBytes();
            if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
              strings_ = new org.jetbrains.kotlin.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000001;
            }
            strings_.add(bs);
            break;
          }
        }
      }
    } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException(
          e.getMessage()).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
        strings_ = strings_.getUnmodifiableView();
      }
      try {
        unknownFieldsCodedOutput.flush();
      } catch (java.io.IOException e) {
      // Should not happen
      } finally {
        unknownFields = unknownFieldsOutput.toByteString();
      }
      makeExtensionsImmutable();
    }
  }
  public static org.jetbrains.kotlin.protobuf.Parser<StringTable> PARSER =
      new org.jetbrains.kotlin.protobuf.AbstractParser<StringTable>() {
    public StringTable parsePartialFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return new StringTable(input, extensionRegistry);
    }
  };

  @java.lang.Override
  public org.jetbrains.kotlin.protobuf.Parser<StringTable> getParserForType() {
    return PARSER;
  }

  public static final int STRINGS_FIELD_NUMBER = 1;
  private org.jetbrains.kotlin.protobuf.LazyStringList strings_;
  /**
   * <code>repeated string strings = 1;</code>
   */
  public org.jetbrains.kotlin.protobuf.ProtocolStringList
      getStringsList() {
    return strings_;
  }
  /**
   * <code>repeated string strings = 1;</code>
   */
  public int getStringsCount() {
    return strings_.size();
  }
  /**
   * <code>repeated string strings = 1;</code>
   */
  public java.lang.String getStrings(int index) {
    return strings_.get(index);
  }
  /**
   * <code>repeated string strings = 1;</code>
   */
  public org.jetbrains.kotlin.protobuf.ByteString
      getStringsBytes(int index) {
    return strings_.getByteString(index);
  }

  private void initFields() {
    strings_ = org.jetbrains.kotlin.protobuf.LazyStringArrayList.EMPTY;
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(org.jetbrains.kotlin.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    for (int i = 0; i < strings_.size(); i++) {
      output.writeBytes(1, strings_.getByteString(i));
    }
    output.writeRawBytes(unknownFields);
  }

  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;

    size = 0;
    {
      int dataSize = 0;
      for (int i = 0; i < strings_.size(); i++) {
        dataSize += org.jetbrains.kotlin.protobuf.CodedOutputStream
          .computeBytesSizeNoTag(strings_.getByteString(i));
      }
      size += dataSize;
      size += 1 * getStringsList().size();
    }
    size += unknownFields.size();
    memoizedSerializedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  @java.lang.Override
  protected java.lang.Object writeReplace()
      throws java.io.ObjectStreamException {
    return super.writeReplace();
  }

  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(byte[] data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(
      byte[] data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseDelimitedFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(org.jetbrains.kotlin.backend.common.serialization.proto.StringTable prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }

  /**
   * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.StringTable}
   */
  public static final class Builder extends
      org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder<
        org.jetbrains.kotlin.backend.common.serialization.proto.StringTable, Builder>
      implements
      // @@protoc_insertion_point(builder_implements:org.jetbrains.kotlin.backend.common.serialization.proto.StringTable)
      org.jetbrains.kotlin.backend.common.serialization.proto.StringTableOrBuilder {
    // Construct using org.jetbrains.kotlin.backend.common.serialization.proto.StringTable.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
    }
    private static Builder create() {
      return new Builder();
    }

    public Builder clear() {
      super.clear();
      strings_ = org.jetbrains.kotlin.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.StringTable getDefaultInstanceForType() {
      return org.jetbrains.kotlin.backend.common.serialization.proto.StringTable.getDefaultInstance();
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.StringTable build() {
      org.jetbrains.kotlin.backend.common.serialization.proto.StringTable result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.StringTable buildPartial() {
      org.jetbrains.kotlin.backend.common.serialization.proto.StringTable result = new org.jetbrains.kotlin.backend.common.serialization.proto.StringTable(this);
      int from_bitField0_ = bitField0_;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        strings_ = strings_.getUnmodifiableView();
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.strings_ = strings_;
      return result;
    }

    public Builder mergeFrom(org.jetbrains.kotlin.backend.common.serialization.proto.StringTable other) {
      if (other == org.jetbrains.kotlin.backend.common.serialization.proto.StringTable.getDefaultInstance()) return this;
      if (!other.strings_.isEmpty()) {
        if (strings_.isEmpty()) {
          strings_ = other.strings_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensureStringsIsMutable();
          strings_.addAll(other.strings_);
        }
        
      }
      setUnknownFields(
          getUnknownFields().concat(other.unknownFields));
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.jetbrains.kotlin.backend.common.serialization.proto.StringTable parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.kotlin.backend.common.serialization.proto.StringTable) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private org.jetbrains.kotlin.protobuf.LazyStringList strings_ = org.jetbrains.kotlin.protobuf.LazyStringArrayList.EMPTY;
    private void ensureStringsIsMutable() {
      if (!((bitField0_ & 0x00000001) == 0x00000001)) {
        strings_ = new org.jetbrains.kotlin.protobuf.LazyStringArrayList(strings_);
        bitField0_ |= 0x00000001;
       }
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public org.jetbrains.kotlin.protobuf.ProtocolStringList
        getStringsList() {
      return strings_.getUnmodifiableView();
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public int getStringsCount() {
      return strings_.size();
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public java.lang.String getStrings(int index) {
      return strings_.get(index);
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public org.jetbrains.kotlin.protobuf.ByteString
        getStringsBytes(int index) {
      return strings_.getByteString(index);
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public Builder setStrings(
        int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureStringsIsMutable();
      strings_.set(index, value);
      
      return this;
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public Builder addStrings(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureStringsIsMutable();
      strings_.add(value);
      
      return this;
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public Builder addAllStrings(
        java.lang.Iterable<java.lang.String> values) {
      ensureStringsIsMutable();
      org.jetbrains.kotlin.protobuf.AbstractMessageLite.Builder.addAll(
          values, strings_);
      
      return this;
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public Builder clearStrings() {
      strings_ = org.jetbrains.kotlin.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      
      return this;
    }
    /**
     * <code>repeated string strings = 1;</code>
     */
    public Builder addStringsBytes(
        org.jetbrains.kotlin.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureStringsIsMutable();
      strings_.add(value);
      
      return this;
    }

    // @@protoc_insertion_point(builder_scope:org.jetbrains.kotlin.backend.common.serialization.proto.StringTable)
  }

  static {
    defaultInstance = new StringTable(true);
    defaultInstance.initFields();
  }

  // @@protoc_insertion_point(class_scope:org.jetbrains.kotlin.backend.common.serialization.proto.StringTable)
}
