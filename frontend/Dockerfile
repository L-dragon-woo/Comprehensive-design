FROM node:22-alpine

WORKDIR /app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

RUN corepack enable && corepack prepare pnpm@10.24.0 --activate

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile --config.ignore-scripts=false

COPY . .

ARG VITE_KAKAO_MAP_APP_KEY=""
ARG VITE_KAKAO_JAVASCRIPT_KEY=""
ENV VITE_KAKAO_MAP_APP_KEY=$VITE_KAKAO_MAP_APP_KEY
ENV VITE_KAKAO_JAVASCRIPT_KEY=$VITE_KAKAO_JAVASCRIPT_KEY

RUN pnpm build

EXPOSE 5173

CMD ["pnpm", "preview", "--host", "0.0.0.0", "--port", "5173"]
