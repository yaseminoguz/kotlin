// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

/**
 * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer}
 */
public final class IrDeclarationContainer extends
    org.jetbrains.kotlin.protobuf.GeneratedMessageLite implements
    // @@protoc_insertion_point(message_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer)
    IrDeclarationContainerOrBuilder {
  // Use IrDeclarationContainer.newBuilder() to construct.
  private IrDeclarationContainer(org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder builder) {
    super(builder);
    this.unknownFields = builder.getUnknownFields();
  }
  private IrDeclarationContainer(boolean noInit) { this.unknownFields = org.jetbrains.kotlin.protobuf.ByteString.EMPTY;}

  private static final IrDeclarationContainer defaultInstance;
  public static IrDeclarationContainer getDefaultInstance() {
    return defaultInstance;
  }

  public IrDeclarationContainer getDefaultInstanceForType() {
    return defaultInstance;
  }

  private final org.jetbrains.kotlin.protobuf.ByteString unknownFields;
  private IrDeclarationContainer(
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
            if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
              declaration_ = new java.util.ArrayList<org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration>();
              mutable_bitField0_ |= 0x00000001;
            }
            declaration_.add(input.readMessage(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.PARSER, extensionRegistry));
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
        declaration_ = java.util.Collections.unmodifiableList(declaration_);
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
  public static org.jetbrains.kotlin.protobuf.Parser<IrDeclarationContainer> PARSER =
      new org.jetbrains.kotlin.protobuf.AbstractParser<IrDeclarationContainer>() {
    public IrDeclarationContainer parsePartialFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return new IrDeclarationContainer(input, extensionRegistry);
    }
  };

  @java.lang.Override
  public org.jetbrains.kotlin.protobuf.Parser<IrDeclarationContainer> getParserForType() {
    return PARSER;
  }

  public static final int DECLARATION_FIELD_NUMBER = 1;
  private java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration> declaration_;
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
   */
  public java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration> getDeclarationList() {
    return declaration_;
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
   */
  public java.util.List<? extends org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationOrBuilder> 
      getDeclarationOrBuilderList() {
    return declaration_;
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
   */
  public int getDeclarationCount() {
    return declaration_.size();
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration getDeclaration(int index) {
    return declaration_.get(index);
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationOrBuilder getDeclarationOrBuilder(
      int index) {
    return declaration_.get(index);
  }

  private void initFields() {
    declaration_ = java.util.Collections.emptyList();
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    for (int i = 0; i < getDeclarationCount(); i++) {
      if (!getDeclaration(i).isInitialized()) {
        memoizedIsInitialized = 0;
        return false;
      }
    }
    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(org.jetbrains.kotlin.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    for (int i = 0; i < declaration_.size(); i++) {
      output.writeMessage(1, declaration_.get(i));
    }
    output.writeRawBytes(unknownFields);
  }

  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;

    size = 0;
    for (int i = 0; i < declaration_.size(); i++) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeMessageSize(1, declaration_.get(i));
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

  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(byte[] data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(
      byte[] data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseDelimitedFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }

  /**
   * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer}
   */
  public static final class Builder extends
      org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder<
        org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer, Builder>
      implements
      // @@protoc_insertion_point(builder_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer)
      org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainerOrBuilder {
    // Construct using org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer.newBuilder()
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
      declaration_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer getDefaultInstanceForType() {
      return org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer.getDefaultInstance();
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer build() {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer buildPartial() {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer result = new org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer(this);
      int from_bitField0_ = bitField0_;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        declaration_ = java.util.Collections.unmodifiableList(declaration_);
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.declaration_ = declaration_;
      return result;
    }

    public Builder mergeFrom(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer other) {
      if (other == org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer.getDefaultInstance()) return this;
      if (!other.declaration_.isEmpty()) {
        if (declaration_.isEmpty()) {
          declaration_ = other.declaration_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensureDeclarationIsMutable();
          declaration_.addAll(other.declaration_);
        }
        
      }
      setUnknownFields(
          getUnknownFields().concat(other.unknownFields));
      return this;
    }

    public final boolean isInitialized() {
      for (int i = 0; i < getDeclarationCount(); i++) {
        if (!getDeclaration(i).isInitialized()) {
          
          return false;
        }
      }
      return true;
    }

    public Builder mergeFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration> declaration_ =
      java.util.Collections.emptyList();
    private void ensureDeclarationIsMutable() {
      if (!((bitField0_ & 0x00000001) == 0x00000001)) {
        declaration_ = new java.util.ArrayList<org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration>(declaration_);
        bitField0_ |= 0x00000001;
       }
    }

    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration> getDeclarationList() {
      return java.util.Collections.unmodifiableList(declaration_);
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public int getDeclarationCount() {
      return declaration_.size();
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration getDeclaration(int index) {
      return declaration_.get(index);
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder setDeclaration(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureDeclarationIsMutable();
      declaration_.set(index, value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder setDeclaration(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.Builder builderForValue) {
      ensureDeclarationIsMutable();
      declaration_.set(index, builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder addDeclaration(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureDeclarationIsMutable();
      declaration_.add(value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder addDeclaration(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureDeclarationIsMutable();
      declaration_.add(index, value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder addDeclaration(
        org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.Builder builderForValue) {
      ensureDeclarationIsMutable();
      declaration_.add(builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder addDeclaration(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.Builder builderForValue) {
      ensureDeclarationIsMutable();
      declaration_.add(index, builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder addAllDeclaration(
        java.lang.Iterable<? extends org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration> values) {
      ensureDeclarationIsMutable();
      org.jetbrains.kotlin.protobuf.AbstractMessageLite.Builder.addAll(
          values, declaration_);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder clearDeclaration() {
      declaration_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000001);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration declaration = 1;</code>
     */
    public Builder removeDeclaration(int index) {
      ensureDeclarationIsMutable();
      declaration_.remove(index);

      return this;
    }

    // @@protoc_insertion_point(builder_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer)
  }

  static {
    defaultInstance = new IrDeclarationContainer(true);
    defaultInstance.initFields();
  }

  // @@protoc_insertion_point(class_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationContainer)
}
