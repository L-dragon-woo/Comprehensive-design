#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-ap-northeast-2}"
CLUSTER_NAME="${CLUSTER_NAME:-skinai-cluster}"
NAMESPACE="${NAMESPACE:-skinai}"
SNS_TOPIC_NAME="${SNS_TOPIC_NAME:-skinai-alerts}"
SNS_EMAIL="${SNS_EMAIL:-}"
ENABLE_SNS_ALARMS="${ENABLE_SNS_ALARMS:-false}"

RDS_INSTANCE_ID="${RDS_INSTANCE_ID:-}"
ELASTICACHE_CLUSTER_ID="${ELASTICACHE_CLUSTER_ID:-}"
LOAD_BALANCER_FULL_NAME="${LOAD_BALANCER_FULL_NAME:-}"
TARGET_GROUP_FULL_NAME="${TARGET_GROUP_FULL_NAME:-}"

RDS_CPU_THRESHOLD="${RDS_CPU_THRESHOLD:-80}"
RDS_CONNECTIONS_THRESHOLD="${RDS_CONNECTIONS_THRESHOLD:-80}"
RDS_FREE_STORAGE_THRESHOLD_BYTES="${RDS_FREE_STORAGE_THRESHOLD_BYTES:-1073741824}"
ALB_5XX_THRESHOLD="${ALB_5XX_THRESHOLD:-5}"
ELASTICACHE_CPU_THRESHOLD="${ELASTICACHE_CPU_THRESHOLD:-80}"
ELASTICACHE_MEMORY_THRESHOLD="${ELASTICACHE_MEMORY_THRESHOLD:-80}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

text_or_empty() {
  if [[ "${1:-}" == "None" || "${1:-}" == "null" ]]; then
    echo ""
  else
    echo "${1:-}"
  fi
}

metric_exists() {
  local namespace="$1"
  local metric_name="$2"
  shift 2
  local output
  output="$(aws cloudwatch list-metrics \
    --region "$REGION" \
    --namespace "$namespace" \
    --metric-name "$metric_name" \
    "$@" \
    --query 'length(Metrics)' \
    --output text)"
  [[ "$output" != "0" ]]
}

put_alarm() {
  local alarm_name="$1"
  shift

  local action_args=()
  if [[ "$ENABLE_SNS_ALARMS" == "true" ]]; then
    action_args=(--alarm-actions "$TOPIC_ARN" --ok-actions "$TOPIC_ARN")
  fi

  aws cloudwatch put-metric-alarm \
    --region "$REGION" \
    --alarm-name "$alarm_name" \
    "${action_args[@]}" \
    "$@"

  echo "Configured alarm: $alarm_name"
}

require_cmd aws

TOPIC_ARN=""

if [[ "$ENABLE_SNS_ALARMS" == "true" ]]; then
  TOPIC_ARN="$(aws sns create-topic \
    --region "$REGION" \
    --name "$SNS_TOPIC_NAME" \
    --query 'TopicArn' \
    --output text)"

  echo "SNS topic: $TOPIC_ARN"

  if [[ -n "$SNS_EMAIL" ]]; then
    aws sns subscribe \
      --region "$REGION" \
      --topic-arn "$TOPIC_ARN" \
      --protocol email \
      --notification-endpoint "$SNS_EMAIL" >/dev/null
    echo "SNS email subscription requested. Confirm it from your mailbox: $SNS_EMAIL"
  fi
else
  echo "SNS alarm actions disabled. Alarms will be visible in CloudWatch only."
fi

if [[ -z "$LOAD_BALANCER_FULL_NAME" ]] && command -v kubectl >/dev/null 2>&1; then
  FRONTEND_LB_DNS="$(kubectl get svc -n "$NAMESPACE" frontend -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)"
  if [[ -n "$FRONTEND_LB_DNS" ]]; then
    LB_ARN="$(aws elbv2 describe-load-balancers \
      --region "$REGION" \
      --query "LoadBalancers[?DNSName=='$FRONTEND_LB_DNS'].LoadBalancerArn | [0]" \
      --output text)"
    LB_ARN="$(text_or_empty "$LB_ARN")"
    if [[ -n "$LB_ARN" ]]; then
      LOAD_BALANCER_FULL_NAME="${LB_ARN#*loadbalancer/}"
      TARGET_GROUP_ARN="$(aws elbv2 describe-target-groups \
        --region "$REGION" \
        --load-balancer-arn "$LB_ARN" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)"
      TARGET_GROUP_ARN="$(text_or_empty "$TARGET_GROUP_ARN")"
      if [[ -n "$TARGET_GROUP_ARN" ]]; then
        TARGET_GROUP_FULL_NAME="${TARGET_GROUP_ARN#*targetgroup/}"
      fi
    fi
  fi
fi

if [[ -z "$RDS_INSTANCE_ID" ]]; then
  RDS_INSTANCE_ID="$(aws rds describe-db-instances \
    --region "$REGION" \
    --query "DBInstances[?contains(DBInstanceIdentifier, 'skinai')].DBInstanceIdentifier | [0]" \
    --output text)"
  RDS_INSTANCE_ID="$(text_or_empty "$RDS_INSTANCE_ID")"
fi

if [[ -n "$LOAD_BALANCER_FULL_NAME" ]]; then
  put_alarm "skinai-alb-target-5xx" \
    --namespace AWS/ApplicationELB \
    --metric-name HTTPCode_Target_5XX_Count \
    --statistic Sum \
    --period 60 \
    --evaluation-periods 3 \
    --datapoints-to-alarm 2 \
    --threshold "$ALB_5XX_THRESHOLD" \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=LoadBalancer,Value="$LOAD_BALANCER_FULL_NAME" \
    --treat-missing-data notBreaching
else
  echo "Skipped ALB 5xx alarm. Set LOAD_BALANCER_FULL_NAME or run where kubectl can read svc/frontend."
fi

if [[ -n "$LOAD_BALANCER_FULL_NAME" && -n "$TARGET_GROUP_FULL_NAME" ]]; then
  put_alarm "skinai-alb-unhealthy-targets" \
    --namespace AWS/ApplicationELB \
    --metric-name UnHealthyHostCount \
    --statistic Average \
    --period 60 \
    --evaluation-periods 2 \
    --datapoints-to-alarm 2 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=LoadBalancer,Value="$LOAD_BALANCER_FULL_NAME" Name=TargetGroup,Value="$TARGET_GROUP_FULL_NAME" \
    --treat-missing-data notBreaching
else
  echo "Skipped ALB unhealthy target alarm. Set TARGET_GROUP_FULL_NAME if auto-discovery did not find it."
fi

if [[ -n "$RDS_INSTANCE_ID" ]]; then
  put_alarm "skinai-rds-cpu-high" \
    --namespace AWS/RDS \
    --metric-name CPUUtilization \
    --statistic Average \
    --period 60 \
    --evaluation-periods 5 \
    --datapoints-to-alarm 3 \
    --threshold "$RDS_CPU_THRESHOLD" \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=DBInstanceIdentifier,Value="$RDS_INSTANCE_ID" \
    --treat-missing-data notBreaching

  put_alarm "skinai-rds-connections-high" \
    --namespace AWS/RDS \
    --metric-name DatabaseConnections \
    --statistic Average \
    --period 60 \
    --evaluation-periods 5 \
    --datapoints-to-alarm 3 \
    --threshold "$RDS_CONNECTIONS_THRESHOLD" \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=DBInstanceIdentifier,Value="$RDS_INSTANCE_ID" \
    --treat-missing-data notBreaching

  put_alarm "skinai-rds-free-storage-low" \
    --namespace AWS/RDS \
    --metric-name FreeStorageSpace \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --datapoints-to-alarm 2 \
    --threshold "$RDS_FREE_STORAGE_THRESHOLD_BYTES" \
    --comparison-operator LessThanOrEqualToThreshold \
    --dimensions Name=DBInstanceIdentifier,Value="$RDS_INSTANCE_ID" \
    --treat-missing-data notBreaching
else
  echo "Skipped RDS alarms. Set RDS_INSTANCE_ID if auto-discovery did not find it."
fi

if [[ -n "$ELASTICACHE_CLUSTER_ID" ]]; then
  put_alarm "skinai-elasticache-cpu-high" \
    --namespace AWS/ElastiCache \
    --metric-name CPUUtilization \
    --statistic Average \
    --period 60 \
    --evaluation-periods 5 \
    --datapoints-to-alarm 3 \
    --threshold "$ELASTICACHE_CPU_THRESHOLD" \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=CacheClusterId,Value="$ELASTICACHE_CLUSTER_ID" \
    --treat-missing-data notBreaching

  put_alarm "skinai-elasticache-memory-high" \
    --namespace AWS/ElastiCache \
    --metric-name DatabaseMemoryUsagePercentage \
    --statistic Average \
    --period 60 \
    --evaluation-periods 5 \
    --datapoints-to-alarm 3 \
    --threshold "$ELASTICACHE_MEMORY_THRESHOLD" \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=CacheClusterId,Value="$ELASTICACHE_CLUSTER_ID" \
    --treat-missing-data notBreaching
else
  echo "Skipped ElastiCache alarms. Current Kubernetes config uses in-cluster redis; set ELASTICACHE_CLUSTER_ID after migrating."
fi

if metric_exists ContainerInsights node_cpu_utilization --dimensions Name=ClusterName,Value="$CLUSTER_NAME"; then
  put_alarm "skinai-eks-node-cpu-high" \
    --namespace ContainerInsights \
    --metric-name node_cpu_utilization \
    --statistic Average \
    --period 60 \
    --evaluation-periods 5 \
    --datapoints-to-alarm 3 \
    --threshold 80 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --dimensions Name=ClusterName,Value="$CLUSTER_NAME" \
    --treat-missing-data notBreaching
else
  echo "Skipped EKS Container Insights node CPU alarm. Enable the amazon-cloudwatch-observability add-on first."
fi

echo "Done. Review alarms in CloudWatch > Alarms."
