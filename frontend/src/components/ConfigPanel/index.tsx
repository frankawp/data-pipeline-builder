import React, { useEffect, useState } from 'react';
import { Card, Form, Input, InputNumber, Switch, Select, Button, message, Empty, Popconfirm } from 'antd';
import { DeleteOutlined, ApiOutlined } from '@ant-design/icons';
import { usePipelineStore } from '../../store/pipelineStore';
import { connectorApi, transformerApi } from '../../services/api';
import type { ConfigSchema, ConfigField } from '../../types';

const ConfigPanel: React.FC = () => {
  const { selectedNodeId, nodes, updateNode, removeNode } = usePipelineStore();
  const [form] = Form.useForm();
  const [schema, setSchema] = useState<ConfigSchema | null>(null);
  const [loading, setLoading] = useState(false);

  const selectedNode = nodes.find((n) => n.id === selectedNodeId);

  // 加载配置 schema
  useEffect(() => {
    if (!selectedNode) {
      setSchema(null);
      return;
    }

    const loadSchema = async () => {
      setLoading(true);
      try {
        let configSchema: ConfigSchema;
        if (selectedNode.type === 'TRANSFORMER') {
          configSchema = await transformerApi.getSchema(selectedNode.pluginType);
        } else {
          configSchema = await connectorApi.getSchema(selectedNode.pluginType);
        }
        setSchema(configSchema);
        form.setFieldsValue({
          name: selectedNode.name,
          ...selectedNode.config,
        });
      } catch (error) {
        console.error('Failed to load schema:', error);
        message.error('加载配置失败');
      } finally {
        setLoading(false);
      }
    };

    loadSchema();
  }, [selectedNode?.id, selectedNode?.pluginType]);

  // 保存配置
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const { name, ...config } = values;
      updateNode(selectedNodeId!, { name, config });
      message.success('配置已保存');
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  // 测试连接
  const handleTestConnection = async () => {
    if (!selectedNode || selectedNode.type === 'TRANSFORMER') return;

    try {
      const values = await form.validateFields();
      const { name, ...config } = values;
      const result = await connectorApi.testConnection(selectedNode.pluginType, config);
      if (result.success) {
        message.success('连接成功！');
      } else {
        message.error(`连接失败: ${result.message}`);
      }
    } catch (error) {
      message.error('测试连接失败');
    }
  };

  // 删除节点
  const handleDelete = () => {
    if (selectedNodeId) {
      removeNode(selectedNodeId);
      message.success('节点已删除');
    }
  };

  // 渲染表单字段
  const renderField = (field: ConfigField) => {
    const rules = field.required ? [{ required: true, message: `请输入${field.label}` }] : [];

    switch (field.type) {
      case 'STRING':
      case 'FILE_PATH':
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
          >
            <Input placeholder={field.description} />
          </Form.Item>
        );

      case 'PASSWORD':
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
          >
            <Input.Password placeholder={field.description} />
          </Form.Item>
        );

      case 'TEXTAREA':
      case 'SQL':
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
          >
            <Input.TextArea rows={4} placeholder={field.description} />
          </Form.Item>
        );

      case 'NUMBER':
      case 'INTEGER':
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
            initialValue={field.defaultValue}
          >
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
        );

      case 'BOOLEAN':
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            valuePropName="checked"
            initialValue={field.defaultValue}
          >
            <Switch />
          </Form.Item>
        );

      case 'SELECT':
        const options = (field.options?.options as Array<{ value: string; label: string }>) || [];
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
            initialValue={field.defaultValue}
          >
            <Select options={options} />
          </Form.Item>
        );

      case 'JSON':
      case 'COLUMN_MAPPING':
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
          >
            <Input.TextArea
              rows={4}
              placeholder="JSON 格式配置"
            />
          </Form.Item>
        );

      default:
        return (
          <Form.Item
            key={field.name}
            name={field.name}
            label={field.label}
            tooltip={field.description}
            rules={rules}
          >
            <Input />
          </Form.Item>
        );
    }
  };

  if (!selectedNode) {
    return (
      <Card title="配置面板" size="small" style={{ height: '100%' }}>
        <Empty description="请选择一个节点" />
      </Card>
    );
  }

  return (
    <Card
      title={`配置 - ${selectedNode.name}`}
      size="small"
      style={{ height: '100%', overflow: 'auto' }}
      extra={
        <Popconfirm
          title="确定删除该节点？"
          onConfirm={handleDelete}
          okText="确定"
          cancelText="取消"
        >
          <Button type="text" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      }
    >
      <Form
        form={form}
        layout="vertical"
        size="small"
      >
        <Form.Item
          name="name"
          label="节点名称"
          rules={[{ required: true, message: '请输入节点名称' }]}
        >
          <Input />
        </Form.Item>

        {schema?.fields.map(renderField)}

        <Form.Item>
          <div style={{ display: 'flex', gap: 8 }}>
            <Button type="primary" onClick={handleSave}>
              保存配置
            </Button>
            {selectedNode.type !== 'TRANSFORMER' && (
              <Button icon={<ApiOutlined />} onClick={handleTestConnection}>
                测试连接
              </Button>
            )}
          </div>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default ConfigPanel;
